package at.techbee.spectacled.screens.core.data.webdav

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.mapper.ics.parseVJournals
import at.techbee.spectacled.screens.core.mapper.ics.serializeVCalendar
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.domain.SyncState
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.put
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
import io.ktor.http.fullPath
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.http.withCharset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XmlParsingException
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.uuid.ExperimentalUuidApi


//expect suspend fun getCalDavCalendarsDavX5(client: HttpClient, location: Url): List<Principal>

//expect suspend fun getCalendarEntriesDavX5(client: HttpClient, calendar: Calendar): List<Note>


suspend fun discoverPrincipalsMultiplatform(
    client: HttpClient,
    location: Url
): DiscoverPrincipalsResult {

    val principals = mutableSetOf<Principal>()

    val propfindRequest = Propfind(
        prop = WebDavProp(
            currentUserPrincipal = CurrentUserPrincipal()
        )
    )
    val xmlString = calDavXml.encodeToString(propfindRequest)

    client.request(location) {
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml)
        //setBody("<?xml version='1.0' encoding='UTF-8' ?><propfind xmlns=\"DAV:\" xmlns:CAL=\"urn:ietf:params:xml:ns:caldav\" xmlns:CARD=\"urn:ietf:params:xml:ns:carddav\"><prop><current-user-principal /><displayname /></prop></propfind>")
        setBody(xmlString)
    }.let { response ->
        //response = "Response: $httpResponse"

        if(!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> DiscoverPrincipalsResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> DiscoverPrincipalsResult.NotAuthorized
                else -> DiscoverPrincipalsResult.Failed(response.status, "Collections couldn't be fetched.", "${response.status.description} ${response.status.value}")
            }
        }

        try {
            //val responseBody = response.bodyAsText()
            //print("responseBody: $responseBody")
            //val p = xml.decodeFromString<WebDavMultiStatus>(httpResponse.bodyAsText())
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(response.bodyAsText())
            )
            //print("Parsed response: $multistatusResponse")

            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if(propStat.status != "HTTP/1.1 200 OK")
                        return@forEach

                    val href = propStat.prop.currentUserPrincipal?.href ?: return@forEach
                    //val displayName = propStat.prop.displayName

                    val currentPrincipal = Principal(
                        id = 0L,
                        principalUrl = URLBuilder(location).takeFrom(href).build(),
                        displayName = null,
                        calendarUserAddressSet = emptyList()
                    )
                    principals.add(currentPrincipal)
                }
            }
            return DiscoverPrincipalsResult.Success(principals.toList())

        } catch (e: XmlParsingException) {
            println("Parsing failed: ${e.message}")
            println(e.stackTraceToString())
            return DiscoverPrincipalsResult.Failed(response.status, "Collections couldn't be parsed.", e.stackTraceToString())
        }
    }
}



suspend fun discoverHomeCollections(client: HttpClient, principal: Principal): DiscoverHomeCollectionsResult {

    val homeCollections = mutableSetOf<HomeCollection>()
    var principalDisplayName: String? = null
    val principalCalendarUserAddressSet = mutableListOf<String>()

    val propfindRequest = Propfind(
        prop = WebDavProp(
            displayName = "", // An empty string will serialize to <D:displayname/>
            calendarUserAddressSet = CalendarUserAddressSet(),
            calendarHomeSet = CalendarHomeSet()
        )
    )
    val xmlString = calDavXml.encodeToString(propfindRequest)

    client.request(principal.principalUrl) {
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml)
        //setBody("<?xml version='1.0' encoding='UTF-8' ?><propfind xmlns=\"DAV:\" xmlns:CAL=\"urn:ietf:params:xml:ns:caldav\" xmlns:CARD=\"urn:ietf:params:xml:ns:carddav\"><prop><CAL:calendar-home-set /><displayname /></prop></propfind>")
        setBody(xmlString)
    }.let { response ->
        //response = "Response: $httpResponse"

        if(!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> DiscoverHomeCollectionsResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> DiscoverHomeCollectionsResult.NotAuthorized
                else -> DiscoverHomeCollectionsResult.Failed(response.status, "Home Collections couldn't be fetched.", "${response.status.description} ${response.status.value}")
            }
        }

        try {
            val responseBody = response.bodyAsText()
            print("responseBody: $responseBody")

            //val p = xml.decodeFromString<WebDavMultiStatus>(httpResponse.bodyAsText())
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(responseBody)
            )
            //print("Parsed response: $multistatusResponse")

            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if(propStat.status != "HTTP/1.1 200 OK")
                        return@forEach

                    principalDisplayName = propStat.prop.displayName
                    propStat.prop.calendarUserAddressSet?.hrefs?.let { principalCalendarUserAddressSet.addAll(it) }

                    val calendarHomeSets = propStat.prop.calendarHomeSet?.hrefs ?: return@forEach
                    //println("Response $response")
                    //println("CalendarHomeSets " + calendarHomeSets.joinToString(separator = ", "))

                    calendarHomeSets.forEach { calendarHomeSet ->
                        homeCollections.add(
                            HomeCollection(
                                id = 0L,
                                principalId = 0L,
                                url = URLBuilder(principal.principalUrl).takeFrom(calendarHomeSet).build(),
                                calDavPrivileges = emptyList(),  // populated later
                            )
                        )
                    }
                }
            }

            return DiscoverHomeCollectionsResult.Success(principalDisplayName, principalCalendarUserAddressSet, homeCollections.toList())

        } catch (e: XmlParsingException) {
            println("Parsing failed: ${e.message}")
            println(e.stackTraceToString())
            return DiscoverHomeCollectionsResult.Failed(response.status, "Home Collections couldn't be parsed.", e.stackTraceToString())
        }
    }
}



suspend fun discoverCalendars(client: HttpClient, homeCollection: HomeCollection): DiscoverCalendarsResult {

    val calendars = mutableListOf<Calendar>()
    val calDavPrivileges = mutableListOf<CalDavPrivilege>()

    //println("CalendarHomeSet from Set: $homeCollectionUrl")

    val propfindRequest = Propfind(
        prop = WebDavProp(
            displayName = "", // An empty string will serialize to <D:displayname/>
            calendarDescription = "",
            getCTag = "",
            calendarColor = Color.Unspecified,
            supportedCalendarComponentSet = SupportedCalendarComponentSet(),
            resourceType = ResourceType(),
            currentUserPrivilegeSet = CurrentUserPrivilegeSet()
        )
    )
    val xmlString = calDavXml.encodeToString(propfindRequest)

    client.request(homeCollection.url) {
        headers.append(HttpHeaders.Depth, "1")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml)
        //setBody("<?xml version='1.0' encoding='UTF-8' ?><propfind xmlns=\"DAV:\" xmlns:CAL=\"urn:ietf:params:xml:ns:caldav\" xmlns:CARD=\"urn:ietf:params:xml:ns:carddav\"><prop><displayname /><resourcetype /><n0:calendar-color xmlns:n0=\"http://apple.com/ns/ical/\" /><CAL:calendar-description /><current-user-privilege-set /><CAL:supported-calendar-component-set /><n1:getctag xmlns:n1=\"http://calendarserver.org/ns/\" /></prop></propfind>\n")
        //TODO: Add properties again!
        //setBody("<?xml version='1.0' encoding='UTF-8' ?><propfind xmlns=\"DAV:\" xmlns:CAL=\"urn:ietf:params:xml:ns:caldav\" xmlns:CARD=\"urn:ietf:params:xml:ns:carddav\"><prop><displayname /><CAL:calendar-description /><n1:getctag xmlns:n1=\"http://calendarserver.org/ns/\" /></prop></propfind>\n")
        setBody(xmlString)
    }.let { response ->
        //response = "Response: $httpResponse"
        //val xml = XML { recommended() }

        if(!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> DiscoverCalendarsResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> DiscoverCalendarsResult.NotAuthorized
                else -> DiscoverCalendarsResult.Failed(response.status, "Home Collections couldn't be fetched.", "${response.status.description} ${response.status.value}")
            }
        }

        try {
            //val responseBody = response.bodyAsText()
            //print("responseBody: $responseBody")

            //val p = xml.decodeFromString<WebDavMultiStatus>(httpResponse.bodyAsText())
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(response.bodyAsText())
            )
            print("Parsed response: $multistatusResponse")

            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if (propStat.status != "HTTP/1.1 200 OK")  // ignore missing propstat entries
                        return@forEach

                    // if href is the home collection, we take the calDavPrivileges from the response (to later know if we can create and delete calendars)
                    if (URLBuilder(homeCollection.url).takeFrom(response.href).build() == homeCollection.url)
                       propStat.prop.currentUserPrivilegeSet?.privileges?.mapNotNull { CalDavPrivilege.fromTag(it.name) }?.let { calDavPrivileges.addAll(it) }

                    val supportedCalendarComponentSet = propStat.prop.supportedCalendarComponentSet?.let { componentSet ->
                        val calendarComponents = mutableListOf<CalendarComponent>().apply {
                            if(componentSet.components.any { component -> component.name == CalendarComponent.VJOURNAL.name }) add(CalendarComponent.VJOURNAL)
                            if(componentSet.components.any { component -> component.name == CalendarComponent.VTODO.name }) add(CalendarComponent.VTODO)
                            if(componentSet.components.any { component -> component.name == CalendarComponent.VEVENT.name }) add(CalendarComponent.VEVENT)
                        }
                        return@let calendarComponents
                    } ?: emptyList()


                    // skip calendars that are NOT of resource type calendar and skip if there's no calendar with VJOURNAL supported
                    if(propStat.prop.resourceType?.calendar == null || supportedCalendarComponentSet.none { component -> component == CalendarComponent.VJOURNAL })
                        return@forEach

                    /*
                    //val calendarTimezone = response[CalendarTimezone::class.java]
                    println("============================")
                    println("Response $response")
                    println("ResourceType " + calendarResourceType?.types?.joinToString(transform = { "CalendarResourceType: Name: ${it.name}, Namespace: ${it.namespace}; " }))
                    */

                    calendars.add(
                        Calendar(
                            id = 0L,
                            homeCollectionId = 0L,
                            url = URLBuilder(homeCollection.url).takeFrom(response.href).build(),    // TODO: Check if correct!
                            displayName = propStat.prop.displayName,
                            calendarDescription = propStat.prop.calendarDescription,
                            color = propStat.prop.calendarColor,
                            ctag = propStat.prop.getCTag,
                            supportedComponents = supportedCalendarComponentSet,
                            calDavPrivileges = propStat.prop.currentUserPrivilegeSet?.privileges?.mapNotNull { CalDavPrivilege.fromTag(it.name) }?: emptyList(),
                            calendarSyncStatus = null,
                            syncToken = null
                        )
                    )
                }
            }

            return DiscoverCalendarsResult.Success(calDavPrivileges, calendars)

        } catch (e: XmlParsingException) {
            println("Parsing failed: ${e.message}")
            println(e.stackTraceToString())
            return DiscoverCalendarsResult.Failed(response.status, "Calendars couldn't be parsed.", e.stackTraceToString())
        }
    }
}


suspend fun multigetResourceHrefsMultiplatform(
    client: HttpClient,
    calendar: Calendar
): MultigetResourceHrefETagResult {

    val journalFilter = CompFilter(name = "VJOURNAL")
    val calendarFilter = CompFilter(name = "VCALENDAR", compFilter = journalFilter)
    val mainFilter = CalFilter(compFilter = calendarFilter)

    val calendarQuery = CalendarQuery(
        prop = WebDavProp(getETag = ""),
        filter = mainFilter
    )
    val xmlString = calDavXml.encodeToString(calendarQuery)

    client.request(calendar.url) {
        headers.append(HttpHeaders.Depth, "1")
        method = HttpMethod.parse("REPORT")
        contentType(ContentType.Application.Xml)
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
            //val p = xml.decodeFromString<WebDavMultiStatus>(httpResponse.bodyAsText())
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(response.bodyAsText())
            )
            //print("Parsed response: $multistatusResponse")

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
            //print("CalendarHrefs: $calendarHrefs")
        } catch (e: XmlParsingException) {
            println("Parsing failed: ${e.message}")
            println(e.stackTraceToString())
            return MultigetResourceHrefETagResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
}


suspend fun syncCollectionMultiplatform(
    client: HttpClient,
    calendar: Calendar
): MultigetSyncCollectionResult {

    val syncCollection = SyncCollection(syncToken = calendar.syncToken ?: "")
    val xmlString = calDavXml.encodeToString(syncCollection)

    client.request(calendar.url) {
        headers.append(HttpHeaders.Depth, "1")
        method = HttpMethod.parse("REPORT")
        contentType(ContentType.Application.Xml)
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
            //val p = xml.decodeFromString<WebDavMultiStatus>(httpResponse.bodyAsText())
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
            println("Parsing failed: ${e.message}")
            println(e.stackTraceToString())
            return MultigetSyncCollectionResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
}


suspend fun fetchSingleEntryMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    href: Url
): MultigetResourceResult {

    // 1. Define the properties to fetch for the multiget request.
    val multigetProp = WebDavProp(
        contentType = "",
        getETag = "",
        scheduleTag = "",
        calendarData = ""
    )

    // 2. Create the main CalendarMultiGet request object.
    val calendarMultigetRequest = CalendarMultiget(
        prop = multigetProp,
        hrefs = listOf(href.fullPath)
    )

    val xmlBody = calDavXml.encodeToString(calendarMultigetRequest)

    client.request(calendar.url) {
        method = HttpMethod.parse("REPORT")
        setBody(xmlBody)
    }.let { response ->
        //response = "Response: $httpResponse"
        //val xml = XML { recommended() }

        if (!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> MultigetResourceResult.NotFound
                else -> MultigetResourceResult.Failed(response.status, "Calendar couldn't be fetched.", "${response.status.description} ${response.status.value}" )
            }
        }

        try {
            val icalEntries = mutableListOf<IcalEntry>()
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(response.bodyAsText())
            )
            //print("Parsed response: $multistatusResponse")
            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->
                    if(propStat.status == "HTTP/1.1 200 OK") {
                        val parsedIcalEntries = propStat.prop.calendarData?.let { parseVJournals(it) } ?: return@forEach
                        val href = URLBuilder(calendar.url).takeFrom(response.href).build()
                        parsedIcalEntries.forEach { icalEntries.add(it.copy(etag = propStat.prop.getETag, href = href)) }
                    }
                }
            }
            println("--- End of Calendar Data ---")
            return MultigetResourceResult.Success(icalEntries)

        } catch (e: XmlParsingException) {
            println("Parsing failed: ${e.message}")
            println(e.stackTraceToString())
            return MultigetResourceResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
}


@OptIn(ExperimentalUuidApi::class)
suspend fun createCalendarMultiplatform(
    client: HttpClient,
    newCalendar: Calendar
): UpsertCalendarResult {

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
                    components = listOf(CalendarComp(name = CalendarComponent.VJOURNAL.name))
                ),
                currentUserPrincipal = CurrentUserPrincipal()
            )
        )
    )
    val xmlString = calDavXml.encodeToString(mkColRequest)

    client.request(newCalendar.url.toString().trimEnd('/')+"/") {
        //headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("MKCOL")
        contentType(ContentType.Application.Xml.withParameter("charset", "utf-8"))
        accept(ContentType.Application.Xml)
        setBody(xmlString)
    }.let { response ->


        if(!response.status.isSuccess()) {
            return when (response.status) {
                HttpStatusCode.Forbidden ->    // Missing bind privilege on calendar-home-set
                    UpsertCalendarResult.Failed(response.status, "Calendar couldn't be created due to missing privileges.", "${response.status.description} ${response.status.value}")

                HttpStatusCode.Conflict ->
                    UpsertCalendarResult.Failed(response.status,"Parent does not exist or calendar already exists.", "${response.status.description} ${response.status.value}")

                HttpStatusCode.MethodNotAllowed ->       // Server does not allow MKCOL
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
            supportedCalendarComponentSet = SupportedCalendarComponentSet(),
            resourceType = ResourceType(),
            currentUserPrivilegeSet = CurrentUserPrivilegeSet()
        )
    )
    val xmlString2 = calDavXml.encodeToString(propfindRequest)

    client.request(newCalendar.url) {
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml)
        setBody(xmlString2)
    }.let { response ->

        if (!response.status.isSuccess())
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be fetched.", "${response.status.description} ${response.status.value}")

        try {
            val responseBody = response.bodyAsText()
            print("responseBody: $responseBody")

            //val p = xml.decodeFromString<WebDavMultiStatus>(httpResponse.bodyAsText())
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(responseBody)
            )
            print("Parsed response: $multistatusResponse")

            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if (propStat.status != "HTTP/1.1 200 OK")  // ignore missing propstat entries
                        return@forEach

                    val supportedCalendarComponentSet = propStat.prop.supportedCalendarComponentSet?.let { componentSet ->
                        val calendarComponents = mutableListOf<CalendarComponent>().apply {
                            if (componentSet.components.any { component -> component.name == CalendarComponent.VJOURNAL.name }) add(
                                CalendarComponent.VJOURNAL
                            )
                            if (componentSet.components.any { component -> component.name == CalendarComponent.VTODO.name }) add(
                                CalendarComponent.VTODO
                            )
                            if (componentSet.components.any { component -> component.name == CalendarComponent.VEVENT.name }) add(
                                CalendarComponent.VEVENT
                            )
                        }
                        return@let calendarComponents
                    } ?: emptyList()


                    // skip calendars that are NOT of resource type calendar and skip if there's no calendar with VJOURNAL supported
                    if (propStat.prop.resourceType?.calendar == null || supportedCalendarComponentSet.none { component -> component == CalendarComponent.VJOURNAL })
                        UpsertCalendarResult.Failed(HttpStatusCode.UnprocessableEntity, "Creation of calendar with supported component failed.")
                    // TODO: Delete calendar in this case?

                    return UpsertCalendarResult.Success(newCalendar.copy(
                        url = URLBuilder(newCalendar.url).takeFrom(response.href).build(),
                        displayName = propStat.prop.displayName,
                        calendarDescription = propStat.prop.calendarDescription,
                        color = propStat.prop.calendarColor,
                        ctag = propStat.prop.getCTag,
                        supportedComponents = supportedCalendarComponentSet,
                        calDavPrivileges = propStat.prop.currentUserPrivilegeSet?.privileges?.mapNotNull { CalDavPrivilege.fromTag(it.name) }
                            ?: emptyList()
                    ))
                }
            }

        } catch (e: XmlParsingException) {
            println("Parsing failed: ${e.message}")
            println(e.stackTraceToString())
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
    return UpsertCalendarResult.Failed(HttpStatusCode.UnprocessableEntity, "Unknown error occurred.")
}


@OptIn(ExperimentalUuidApi::class)
suspend fun updateCalDavCalendarMultiplatform(
    client: HttpClient,
    calendar: Calendar
): UpsertCalendarResult {

    val propertyupdateRequest = CalendarPropertyupdate(
        set = WebDavSet(
            prop = WebDavProp(
                displayName = calendar.displayName ?: "",
                calendarDescription = calendar.calendarDescription,
                calendarColor = calendar.color,
            )
        )
    )
    val xmlString = calDavXml.encodeToString(propertyupdateRequest)

    client.request(calendar.url.toString().trimEnd('/')+"/") {
        method = HttpMethod.parse("PROPPATCH")
        contentType(ContentType.Application.Xml.withParameter("charset", "utf-8"))
        accept(ContentType.Application.Xml)
        setBody(xmlString)
    }.let { response ->


        if(!response.status.isSuccess()) {
            return when (response.status) {
                HttpStatusCode.Forbidden ->    // Missing bind privilege on calendar-home-set
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
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml)
        setBody(xmlString2)
    }.let { response ->

        if (!response.status.isSuccess())
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be fetched.", "${response.status.description} ${response.status.value}")

        try {
            val responseBody = response.bodyAsText()
            print("responseBody: $responseBody")

            //val p = xml.decodeFromString<WebDavMultiStatus>(httpResponse.bodyAsText())
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(responseBody)
            )
            print("Parsed response: $multistatusResponse")

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
            println("Parsing failed: ${e.message}")
            //println(e.stackTraceToString())
            return UpsertCalendarResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
    return UpsertCalendarResult.Failed(HttpStatusCode.UnprocessableEntity, "Unknown error occurred.")
}




@OptIn(ExperimentalUuidApi::class)
suspend fun deleteCalendarMultiplatform(
    client: HttpClient,
    calendar: Calendar
): DeleteCalendarResult {

    client.request(calendar.url.toString().trimEnd('/')+"/") {
        //headers.append(HttpHeaders.Depth, "0")
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


@OptIn(ExperimentalUuidApi::class)
suspend fun putResourceMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    icalEntry: IcalEntry
): PutResourceResult {

    val href = Url(calendar.url.toString().trimEnd('/')+"/"+icalEntry.uid+".ics")

    client.put(href) {
        contentType(ContentType.parse("text/calendar").withCharset(Charsets.UTF_8))
        setBody(serializeVCalendar(icalEntry))
        headers.apply {
            if(icalEntry.etag != null)    // send etag or * if a new entry should be created
                append(HttpHeaders.IfMatch, icalEntry.etag)     // update
            else
                append(HttpHeaders.IfNoneMatch, "*")       // insert
        }
    }.let { response ->

        return when(response.status.value) {
            in 200 .. 299 -> PutResourceResult.Success(
                icalEntry.copy(
                    etag = response.headers[HttpHeaders.ETag],
                    href = href,
                    syncState = SyncState.SYNCED
                )
            )
            HttpStatusCode.PreconditionFailed.value -> PutResourceResult.Conflict  // Entry was updated on server
            HttpStatusCode.NotFound.value -> PutResourceResult.NotFound
            else -> PutResourceResult.Failed(response.status, "Creating entry failed on server.", "${response.status.description} ${response.status.value}")
        }
    }
}



@OptIn(ExperimentalUuidApi::class)
suspend fun deleteResourceMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    icalEntry: IcalEntry
): DeleteResourceResult {

    val href = Url(calendar.url.toString().trimEnd('/')+"/"+icalEntry.uid+".ics")

    client.delete(href) {
        contentType(ContentType.parse("text/calendar").withCharset(Charsets.UTF_8))
        headers.append(HttpHeaders.IfMatch, icalEntry.etag?:"*")
    }.let { response ->

        return when(response.status.value) {
            in 200 .. 299 -> DeleteResourceResult.Success
            HttpStatusCode.NotFound.value -> DeleteResourceResult.AlreadyDeleted    // delete locally
            HttpStatusCode.PreconditionFailed.value -> DeleteResourceResult.Conflict    // fetch update and let user decide
            HttpStatusCode.Unauthorized.value, HttpStatusCode.Forbidden.value -> DeleteResourceResult.Failed(response.status, "Entry couldn't be updated due to missing privileges.", "${response.status.description} ${response.status.value}")
            else -> DeleteResourceResult.Failed(response.status, "Creating entry failed on server.", "${response.status.description} ${response.status.value}")
        }
    }
}

suspend fun getResourceMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    icalEntry: IcalEntry
): GetResourceResult {

    val href = Url(calendar.url.toString().trimEnd('/')+"/"+icalEntry.uid+".ics")

    client.get(href) {
        headers.append(HttpHeaders.IfNoneMatch, icalEntry.etag?:"*")
        contentType(ContentType.parse("text/calendar").withCharset(Charsets.UTF_8))
    }.let { response ->
        if(response.status.isSuccess()) {
            val remoteIcalEntry = parseVJournals(response.bodyAsText()).firstOrNull() ?: return GetResourceResult.Failed(response.status, "An unknown error occurred.", "${response.status.description} ${response.status.value}")
            val updatedIcalEntry = remoteIcalEntry.copy(
                id = icalEntry.id,
                calendarId = icalEntry.calendarId,
                etag = response.headers[HttpHeaders.ETag],
                href = href,
                syncState = SyncState.SYNCED
            )
            return GetResourceResult.Success(updatedIcalEntry)
        } else if(response.status == HttpStatusCode.NotFound) {
            return GetResourceResult.NotFound
        } else {
            return GetResourceResult.Failed(response.status, "An unknown error occurred.", "${response.status.description} ${response.status.value}")
        }
    }
}
