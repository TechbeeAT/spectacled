package at.techbee.spectacled.screens.core.data.webdav

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSecure
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.http.withCharsetIfNeeded
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XmlParsingException
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.uuid.ExperimentalUuidApi



@OptIn(ExperimentalUuidApi::class)
suspend fun createCalendarMultiplatform(
    client: HttpClient,
    newCalendar: Calendar,
    credentials: Credentials?
): UpsertCalendarResult {
    if(newCalendar.supportedComponents.none { it == CalendarComponent.VJOURNAL || it == CalendarComponent.VTODO })
        return UpsertCalendarResult.Failed(HttpStatusCode.Forbidden, "supportedComponents not provided")

    val mkColRequest = CalendarMkcol(
        set = WebDavSet(
            prop = WebDavProp(
                resourceType = ResourceType(
                    collection = DavCollection(),
                    calendar = CaldavCalendar()
                ),
                displayName = newCalendar.displayName ?: "",
                calendarDescription = newCalendar.calendarDescription,
                calendarColor = newCalendar.color,
                supportedCalendarComponentSet = SupportedCalendarComponentSet(
                    components = newCalendar.supportedComponents
                        .map { CalendarComp(name = it.name) }
                        .ifEmpty { listOf(CalendarComp(CalendarComponent.VJOURNAL.name), CalendarComp(CalendarComponent.VTODO.name)) }
                ),
                currentUserPrincipal = CurrentUserPrincipal()
            )
        )
    )
    val xmlString = calDavXml.encodeToString(mkColRequest)

    client.request(newCalendar.url.toString().trimEnd('/')+"/") {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        method = HttpMethod.parse("MKCOL")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        accept(ContentType.Application.Xml)
        setBody(xmlString)
    }.let { response ->


        if(!response.status.isSuccess()) {
            return when (response.status) {
                HttpStatusCode.Forbidden ->    // Missing bind privilege on calendar-home-set
                    UpsertCalendarResult.Failed(response.status, "Calendar couldn't be created due to missing privileges.", "${response.status.description} ${response.status.value}")
                HttpStatusCode.Conflict ->
                    UpsertCalendarResult.Failed(response.status,"Parent does not exist or calendar already exists.", "${response.status.description} ${response.status.value}")
                HttpStatusCode.MethodNotAllowed ->
                    UpsertCalendarResult.Failed(response.status, "Server doesn't support creation of new collections.", "${response.status.description} ${response.status.value}")
                else ->
                    UpsertCalendarResult.Failed(response.status, "Creating collection failed.", "${response.status.description} ${response.status.value}")
            }
        }
    }

    // calendar was created, now check if it's really there as expected

    val propfindRequest = Propfind(
        prop = WebDavProp(
            displayName = "", // An empty string will serialize to <D:displayname/>
            calendarDescription = "",
            getCTag = "",
            calendarColor = Color.Unspecified,
            attachmentCollection = HrefProperty(),
            calendarDropbox = HrefProperty(),
            supportedCalendarComponentSet = SupportedCalendarComponentSet(),
            resourceType = ResourceType(),
            currentUserPrivilegeSet = CurrentUserPrivilegeSet()
        )
    )
    val xmlString2 = calDavXml.encodeToString(propfindRequest)

    client.request(newCalendar.url) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlString2)
    }.let { response ->

        if (!response.status.isSuccess())
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be fetched.", "${response.status.description} ${response.status.value}")

        try {
            val responseBody = response.bodyAsText()
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(responseBody)
            )

            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if (propStat.status != "HTTP/1.1 200 OK")
                        return@forEach

                    val supportedCalendarComponentSet = propStat.prop.supportedCalendarComponentSet?.let { componentSet ->
                        val calendarComponents = mutableListOf<CalendarComponent>().apply {
                            if (componentSet.components.any { component -> component.name == CalendarComponent.VJOURNAL.name }) add(CalendarComponent.VJOURNAL)
                            if (componentSet.components.any { component -> component.name == CalendarComponent.VTODO.name }) add(CalendarComponent.VTODO)
                            if (componentSet.components.any { component -> component.name == CalendarComponent.VEVENT.name }) add(CalendarComponent.VEVENT)
                        }
                        return@let calendarComponents
                    } ?: emptyList()


                    // skip calendars that are NOT of resource type calendar and skip if there's no calendar with the requested CalendarComponent supported
                    if (propStat.prop.resourceType?.calendar == null || supportedCalendarComponentSet.none { component -> component == CalendarComponent.VJOURNAL || component == CalendarComponent.VTODO })
                        UpsertCalendarResult.Failed(HttpStatusCode.UnprocessableEntity, "Creation of calendar with supported component failed.")

                    return UpsertCalendarResult.Success(newCalendar.copy(
                        url = URLBuilder(newCalendar.url).takeFrom(response.href).build(),
                        displayName = propStat.prop.displayName,
                        calendarDescription = propStat.prop.calendarDescription,
                        color = propStat.prop.calendarColor,
                        ctag = propStat.prop.getCTag,
                        supportedComponents = supportedCalendarComponentSet,
                        calDavPrivileges = propStat.prop.currentUserPrivilegeSet?.privileges?.mapNotNull { CalDavPrivilege.fromTag(it.name) } ?: emptyList()
                    ))
                }
            }

        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
    return UpsertCalendarResult.Failed(HttpStatusCode.UnprocessableEntity, "Unknown error occurred.")
}


@OptIn(ExperimentalUuidApi::class)
suspend fun updateCalDavCalendarMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    credentials: Credentials?
): UpsertCalendarResult {

    val propertyupdateRequest = CalendarPropertyupdate(
        set = WebDavSet(
            prop = WebDavProp(
                displayName = calendar.displayName ?: "",
                calendarDescription = calendar.calendarDescription,
                calendarColor = calendar.color,
            )
        ),
        remove = if (calendar.color == null) {
            WebDavRemove(
                prop = WebDavProp(
                    calendarColor = Color.Unspecified
                )
            )
        } else null
    )
    val xmlString = calDavXml.encodeToString(propertyupdateRequest)

    client.request(calendar.url.toString().trimEnd('/')+"/") {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        method = HttpMethod.parse("PROPPATCH")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        accept(ContentType.Application.Xml)
        setBody(xmlString)
    }.let { response ->


        if(!response.status.isSuccess()) {
            return when (response.status) {
                HttpStatusCode.Forbidden ->
                    UpsertCalendarResult.Failed(response.status, "Calendar couldn't be updated due to missing privileges.", "${response.status.description} ${response.status.value}")
                HttpStatusCode.Conflict ->
                    UpsertCalendarResult.Failed(response.status, "Calendar couldn't be updated due to conflict.", "${response.status.description} ${response.status.value}")
                HttpStatusCode.NotFound ->
                    UpsertCalendarResult.NotFound
                else ->
                    UpsertCalendarResult.Failed(response.status, "Creating collection failed.", "${response.status.description} ${response.status.value}")
            }
        }
    }

    // calendar was updated, now check if it's really there as expected

    val propfindRequest = Propfind(
        prop = WebDavProp(
            displayName = "", // An empty string will serialize to <D:displayname/>
            calendarDescription = "",
            calendarColor = Color.Unspecified
        )
    )
    val xmlString2 = calDavXml.encodeToString(propfindRequest)

    client.request(calendar.url) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlString2)
    }.let { response ->

        if (!response.status.isSuccess())
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be fetched.", "${response.status.description} ${response.status.value}")

        try {
            val responseBody = response.bodyAsText()
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(responseBody)
            )

            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if (propStat.status != "HTTP/1.1 200 OK")  // ignore missing propstat entries
                        return@forEach

                    return UpsertCalendarResult.Success(
                        calendar.copy(
                            displayName = propStat.prop.displayName,
                            calendarDescription = propStat.prop.calendarDescription,
                            color = propStat.prop.calendarColor
                        )
                    )
                }
            }
        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
    return UpsertCalendarResult.Failed(HttpStatusCode.UnprocessableEntity, "Unknown error occurred.")
}




@OptIn(ExperimentalUuidApi::class)
suspend fun deleteCalendarMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    credentials: Credentials?
): DeleteCalendarResult {

    client.request(calendar.url.toString().trimEnd('/')+"/") {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        method = HttpMethod.Delete
        accept(ContentType.Application.Xml)
    }.let { response ->

        return when {
            response.status.isSuccess() -> DeleteCalendarResult.SuccessfullyDeleted
            response.status == HttpStatusCode.Forbidden ->  // Missing bind privilege on calendar-home-set
                DeleteCalendarResult.Failed(response.status, "Calendar couldn't be deleted due to missing privileges", "Status: ${response.status.description} ${response.status.value}")
            response.status == HttpStatusCode.NotFound ->    // Calendar was already deleted
                DeleteCalendarResult.AlreadyDeleted
            response.status == HttpStatusCode.MethodNotAllowed ->
                DeleteCalendarResult.Failed(response.status, "Server forbids deleting this calendar", "Status: ${response.status.description} ${response.status.value}")
            response.status == HttpStatusCode.Conflict ->
                DeleteCalendarResult.Failed(response.status, "Server forbids deleting this calendar. Locked or server-side constraint.", "Status: ${response.status.description} ${response.status.value}")
            else -> DeleteCalendarResult.Failed(response.status, "Creating collection failed", "Status: ${response.bodyAsText()} (${response.status})")
        }
    }
}




suspend fun multigetResourceHrefsMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    credentials: Credentials?
): MultigetResourceHrefETagResult {
    val componentFilter = calendar.supportedComponents.map { CompFilter(name = it.name) }

    val calendarFilter = CompFilter(name = "VCALENDAR", compFilters = componentFilter)
    val mainFilter = CalFilter(compFilter = calendarFilter)

    val calendarQuery = CalendarQuery(
        prop = WebDavProp(getETag = ""),
        filter = mainFilter
    )
    val xmlString = calDavXml.encodeToString(calendarQuery)

    client.request(calendar.url) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.Depth, "1")
        method = HttpMethod.parse("REPORT")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlString)
    }.let { response ->

        if (!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> MultigetResourceHrefETagResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> MultigetResourceHrefETagResult.NotAuthorized
                else -> MultigetResourceHrefETagResult.Failed(response.status, "Calendar couldn't be fetched.", "${response.status.description} ${response.status.value}" )
            }
        }

        try {
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(response.bodyAsText())
            )

            val hrefMap = mutableMapOf<Url, String?>()
            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->
                    if(propStat.status == "HTTP/1.1 200 OK") {
                        val href = URLBuilder(calendar.url).takeFrom(response.href).build()
                        val eTag = propStat.prop.getETag
                        hrefMap[href] = eTag
                    }
                }
            }
            return MultigetResourceHrefETagResult.Success(hrefMap, multistatusResponse.syncToken)
        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return MultigetResourceHrefETagResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
}

suspend fun syncCollectionMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    credentials: Credentials?
): MultigetSyncCollectionResult {

    val syncCollection = SyncCollection(syncToken = calendar.syncToken ?: "")
    val xmlString = calDavXml.encodeToString(syncCollection)

    client.request(calendar.url) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.Depth, "1")
        method = HttpMethod.parse("REPORT")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlString)
    }.let { response ->

        if (!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> MultigetSyncCollectionResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> MultigetSyncCollectionResult.NotAuthorized
                else -> MultigetSyncCollectionResult.Failed(response.status, "Calendar couldn't be fetched.", "${response.status.description} ${response.status.value}" )
            }
        }

        try {
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(response.bodyAsText())
            )
            print("Parsed response syncCollectionMultiplatform: $multistatusResponse")

            val hrefMap = mutableMapOf<Url, String?>()
            multistatusResponse.responses.forEach { response ->
                val href = URLBuilder(calendar.url).takeFrom(response.href).build()
                val eTag = response.propstat.firstOrNull { it.status == "HTTP/1.1 200 OK" }?.prop?.getETag
                hrefMap[href] = eTag
            }
            return MultigetSyncCollectionResult.Success(syncToken = multistatusResponse.syncToken, hrefMap)
        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return MultigetSyncCollectionResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
}
