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


suspend fun discoverPrincipalsMultiplatform(
    client: HttpClient,
    location: Url,
    credentials: Credentials?
): DiscoverPrincipalsResult {
    val initialResult = discoverPrincipalsInternal(client, location, credentials)

    if (initialResult is DiscoverPrincipalsResult.Success && initialResult.principals.isNotEmpty()) {
        return initialResult
    }

    // If first attempt failed or found no principals, try .well-known/caldav redirect
    if (initialResult is DiscoverPrincipalsResult.NotFound ||
        (initialResult is DiscoverPrincipalsResult.Success && initialResult.principals.isEmpty()) ||
        (initialResult is DiscoverPrincipalsResult.Failed && initialResult.status == HttpStatusCode.MethodNotAllowed)
    ) {
        val wellKnownUrl = URLBuilder(location).apply {
            pathSegments = listOf(".well-known", "caldav")
            parameters.clear()
        }.build()

        if (wellKnownUrl != location) {
            // Nextcloud and others often redirect .well-known/caldav to the actual DAV endpoint.
            // We follow these redirects using a GET request to find the effective discovery URL.
            val discoveryUrl = try {
                val response = client.get(wellKnownUrl) {
                    if (credentials != null) basicAuth(credentials.username, credentials.password)
                }

                if (response.status.value in 300..399) {
                    val redirectUrl = response.headers[HttpHeaders.Location]?.let {
                        URLBuilder(wellKnownUrl).takeFrom(it).build()
                    }
                    if (redirectUrl != null && wellKnownUrl.protocol.isSecure() && !redirectUrl.protocol.isSecure()) {
                        return DiscoverPrincipalsResult.Failed(HttpStatusCode.Forbidden, "HTTPS to HTTP downgrade blocked", "Redirect from ${wellKnownUrl.protocol.name} to ${redirectUrl.protocol.name} was blocked for security reasons.")
                    } else if (redirectUrl != null && (wellKnownUrl.host != redirectUrl.host || wellKnownUrl.protocol != redirectUrl.protocol)) {
                        // SECURITY: Abort if host or scheme changes during discovery
                        return DiscoverPrincipalsResult.Failed(HttpStatusCode.Forbidden, "Cross-domain redirect blocked", "Redirect from ${wellKnownUrl.host} to ${redirectUrl.host} was blocked for security reasons.")
                    } else {
                        redirectUrl ?: wellKnownUrl
                    }
                } else {
                    response.call.request.url
                }
            } catch (_: Exception) {
                wellKnownUrl
            }

            val wellKnownResult = discoverPrincipalsInternal(client, discoveryUrl, credentials)
            if (wellKnownResult is DiscoverPrincipalsResult.Success && wellKnownResult.principals.isNotEmpty()) {
                return wellKnownResult
            }
            // If well-known failed with something other than NotFound, it might be more relevant (e.g., Unauthorized)
            if (wellKnownResult !is DiscoverPrincipalsResult.NotFound) {
                return wellKnownResult
            }
        }
    }

    return initialResult
}

private suspend fun discoverPrincipalsInternal(
    client: HttpClient,
    location: Url,
    credentials: Credentials?,
    redirectCount: Int = 0
): DiscoverPrincipalsResult {
    if (redirectCount > 3) return DiscoverPrincipalsResult.Failed(HttpStatusCode.ServiceUnavailable, "Too many redirects")

    val principals = mutableSetOf<Principal>()

    val propfindRequest = Propfind(
        prop = WebDavProp(
            currentUserPrincipal = CurrentUserPrincipal()
        )
    )
    val xmlString = calDavXml.encodeToString(propfindRequest)

    client.request(location) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlString)
    }.let { httpResponse ->

        // Handle manual redirect for PROPFIND (Ktor default redirect plugin usually only handles GET/HEAD)
        if (httpResponse.status.value in 300..399) {
            val redirectUrl = httpResponse.headers[HttpHeaders.Location]?.let {
                URLBuilder(location).takeFrom(it).build()
            }
            if (redirectUrl != null && redirectUrl != location) {
                if (location.protocol.isSecure() && !redirectUrl.protocol.isSecure()) {
                    return DiscoverPrincipalsResult.Failed(httpResponse.status, "HTTPS to HTTP downgrade blocked")
                }
                // SECURITY: Abort if host or scheme changes
                if (location.host != redirectUrl.host || location.protocol != redirectUrl.protocol) {
                    return DiscoverPrincipalsResult.Failed(httpResponse.status, "Cross-domain redirect blocked", "Redirect from ${location.host} to ${redirectUrl.host} was blocked for security reasons.")
                }

                return discoverPrincipalsInternal(client, redirectUrl, credentials, redirectCount + 1)
            }
        }

        if(!httpResponse.status.isSuccess()) {
            return when(httpResponse.status) {
                HttpStatusCode.NotFound -> DiscoverPrincipalsResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> DiscoverPrincipalsResult.NotAuthorized
                else -> DiscoverPrincipalsResult.Failed(httpResponse.status, "Collections couldn't be fetched.", "${httpResponse.status.description} ${httpResponse.status.value}")
            }
        }

        try {
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(httpResponse.bodyAsText())
            )

            val allResponseCodes = multistatusResponse.responses.flatMap { response -> response.propstat.map { it.status } }
            when {
                allResponseCodes.all { responseCode -> responseCode == "HTTP/1.1 403 Forbidden" } -> return DiscoverPrincipalsResult.NotAuthorized
                allResponseCodes.none { responseCode -> responseCode == "HTTP/1.1 200 OK" } -> return DiscoverPrincipalsResult.Failed(httpResponse.status, "Principal couldn't be processed. None of the response codes returned OK. ", allResponseCodes.joinToString(separator = ", "))
            }


            val effectiveLocation = httpResponse.call.request.url
            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if(propStat.status != "HTTP/1.1 200 OK")
                        return@forEach

                    val href = propStat.prop.currentUserPrincipal?.href ?: return@forEach

                    val currentPrincipal = Principal(
                        id = 0L,
                        principalUrl = URLBuilder(effectiveLocation).takeFrom(href).build(),
                        displayName = null,
                        calendarUserAddressSet = emptyList()
                    )
                    principals.add(currentPrincipal)
                }
            }
            return DiscoverPrincipalsResult.Success(principals.toList())

        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return DiscoverPrincipalsResult.Failed(httpResponse.status, "Collections couldn't be parsed.", e.stackTraceToString())
        }
    }
}

suspend fun discoverHomeCollections(
    client: HttpClient,
    principal: Principal,
    credentials: Credentials?
): DiscoverHomeCollectionsResult {

    val homeCollections = mutableSetOf<HomeCollection>()
    var principalDisplayName: String? = null
    val principalCalendarUserAddressSet = mutableListOf<String>()

    val propfindRequest = Propfind(
        prop = WebDavProp(
            displayName = "", // An empty string will serialize to <D:displayname/>
            calendarUserAddressSet = CalendarUserAddressSet(),
            calendarHomeSet = CalendarHomeSet(),
            attachmentCollection = HrefProperty(),
            dropboxHomeSet = HrefProperty()
        )
    )
    val xmlString = calDavXml.encodeToString(propfindRequest)

    client.request(principal.principalUrl) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.Depth, "0")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlString)
    }.let { response ->

        if(!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> DiscoverHomeCollectionsResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> DiscoverHomeCollectionsResult.NotAuthorized
                else -> DiscoverHomeCollectionsResult.Failed(response.status, "Home Collections couldn't be fetched.", "${response.status.description} ${response.status.value}")
            }
        }

        try {
            val responseBody = response.bodyAsText()
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(responseBody)
            )

            val allResponseCodes = multistatusResponse.responses.flatMap { response -> response.propstat.map { it.status } }
            when {
                allResponseCodes.all { responseCode -> responseCode == "HTTP/1.1 403 Forbidden" } -> return DiscoverHomeCollectionsResult.NotAuthorized
                allResponseCodes.none { responseCode -> responseCode == "HTTP/1.1 200 OK" } -> return DiscoverHomeCollectionsResult.Failed(response.status, "Home Collection couldn't be processed. None of the response codes returned OK. ", allResponseCodes.joinToString(separator = ", "))
            }

            val effectiveLocation = response.call.request.url
            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->

                    if(propStat.status != "HTTP/1.1 200 OK")
                        return@forEach

                    principalDisplayName = propStat.prop.displayName
                    propStat.prop.calendarUserAddressSet?.hrefs?.let { principalCalendarUserAddressSet.addAll(it) }

                    val calendarHomeSets = propStat.prop.calendarHomeSet?.hrefs ?: return@forEach
                    
                    val sharedAttachmentUrl = (propStat.prop.attachmentCollection?.href ?: propStat.prop.dropboxHomeSet?.href)?.let { 
                        URLBuilder(effectiveLocation).takeFrom(it).build() 
                    }

                    calendarHomeSets.forEach { calendarHomeSet ->
                        homeCollections.add(
                            HomeCollection(
                                id = 0L,
                                principalId = 0L,
                                url = URLBuilder(effectiveLocation).takeFrom(calendarHomeSet).build(),
                                calDavPrivileges = emptyList(),  // populated later
                                attachmentCollectionUrl = sharedAttachmentUrl
                            )
                        )
                    }
                }
            }
            
            return DiscoverHomeCollectionsResult.Success(principalDisplayName, principalCalendarUserAddressSet, homeCollections.toList())

        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return DiscoverHomeCollectionsResult.Failed(response.status, "Home Collections couldn't be parsed.", e.stackTraceToString())
        }
    }
}

suspend fun discoverCalendars(
    client: HttpClient,
    homeCollection: HomeCollection,
    credentials: Credentials?
): DiscoverCalendarsResult {

    val calendars = mutableListOf<Calendar>()
    val calDavPrivileges = mutableListOf<CalDavPrivilege>()

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
    val xmlString = calDavXml.encodeToString(propfindRequest)

    client.request(homeCollection.url) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.Depth, "1")
        method = HttpMethod.parse("PROPFIND")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlString)
    }.let { response ->

        if(!response.status.isSuccess()) {
            return when(response.status) {
                HttpStatusCode.NotFound -> DiscoverCalendarsResult.NotFound
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> DiscoverCalendarsResult.NotAuthorized
                else -> DiscoverCalendarsResult.Failed(response.status, "Home Collections couldn't be fetched.", "${response.status.description} ${response.status.value}")
            }
        }

        try {
            val multistatusResponse = calDavXml.decodeFromReader(
                WebDavMultiStatus.serializer(), xmlStreaming.newReader(response.bodyAsText())
            )
            print("Parsed response: $multistatusResponse")

            val allResponseCodes = multistatusResponse.responses.flatMap { response -> response.propstat.map { it.status } }
            when {
                allResponseCodes.all { responseCode -> responseCode == "HTTP/1.1 403 Forbidden" } -> return DiscoverCalendarsResult.NotAuthorized
                allResponseCodes.none { responseCode -> responseCode == "HTTP/1.1 200 OK" } -> return DiscoverCalendarsResult.Failed(response.status, "Calendars couldn't be processed. None of the response codes returned OK. ", allResponseCodes.joinToString(separator = ", "))
            }

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


                    // skip calendars that are NOT of resource type calendar and skip if there's no calendar with the requested supportedCalendarComponent supported
                    if(propStat.prop.resourceType?.calendar == null || supportedCalendarComponentSet.none { it == CalendarComponent.VJOURNAL || it == CalendarComponent.VTODO })
                        return@forEach

                    val attachmentUrl = (propStat.prop.attachmentCollection?.href ?: propStat.prop.calendarDropbox?.href)?.let { 
                        URLBuilder(homeCollection.url).takeFrom(it).build() 
                    } ?: homeCollection.attachmentCollectionUrl

                    calendars.add(
                        Calendar(
                            id = 0L,
                            homeCollectionId = 0L,
                            url = URLBuilder(homeCollection.url).takeFrom(response.href).build(),
                            displayName = propStat.prop.displayName,
                            calendarDescription = propStat.prop.calendarDescription,
                            color = propStat.prop.calendarColor,
                            ctag = propStat.prop.getCTag,
                            supportedComponents = supportedCalendarComponentSet,
                            calDavPrivileges = propStat.prop.currentUserPrivilegeSet?.privileges?.mapNotNull { CalDavPrivilege.fromTag(it.name) }?: emptyList(),
                            calendarSyncStatus = null,
                            syncToken = null,
                            attachmentCollectionUrl = attachmentUrl
                        )
                    )
                }
            }

            return DiscoverCalendarsResult.Success(calDavPrivileges, calendars)

        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return DiscoverCalendarsResult.Failed(response.status, "Calendars couldn't be parsed.", e.stackTraceToString())
        }
    }
}
