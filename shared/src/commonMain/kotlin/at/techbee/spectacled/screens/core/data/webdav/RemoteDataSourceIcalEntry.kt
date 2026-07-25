package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.mapper.ics.parseIcalEntries
import at.techbee.spectacled.screens.core.mapper.ics.serializeVCalendar
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
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
import io.ktor.http.withCharsetIfNeeded
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.serialization.XmlParsingException
import nl.adaptivity.xmlutil.xmlStreaming
import kotlin.uuid.ExperimentalUuidApi



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

suspend fun fetchSingleEntryMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    href: Url,
    credentials: Credentials?,
    fileManager: FileManager?
): MultigetResourceResult {

    val multigetProp = WebDavProp(
        contentType = "",
        getETag = "",
        scheduleTag = "",
        calendarData = ""
    )

    val calendarMultigetRequest = CalendarMultiget(
        prop = multigetProp,
        hrefs = listOf(href.fullPath)
    )

    val xmlBody = calDavXml.encodeToString(calendarMultigetRequest)

    client.request(calendar.url) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        method = HttpMethod.parse("REPORT")
        contentType(ContentType.Application.Xml.withCharsetIfNeeded(Charsets.UTF_8))
        setBody(xmlBody)
    }.let { response ->

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
            multistatusResponse.responses.forEach { response ->
                response.propstat.forEach { propStat ->
                    if(propStat.status == "HTTP/1.1 200 OK") {
                        val parsedIcalEntries = propStat.prop.calendarData?.let { parseIcalEntries(it, fileManager) } ?: return@forEach
                        val hrefResult = URLBuilder(calendar.url).takeFrom(response.href).build()
                        parsedIcalEntries.forEach { icalEntries.add(it.copy(etag = propStat.prop.getETag, href = hrefResult)) }
                    }
                }
            }
            return MultigetResourceResult.Success(icalEntries)

        } catch (e: XmlParsingException) {
            Napier.e("Parsing failed: ${e.message}", e)
            return MultigetResourceResult.Failed(response.status, "Calendar couldn't be parsed.", e.stackTraceToString())
        }
    }
}



@OptIn(ExperimentalUuidApi::class)
suspend fun putResourceMultiplatform(
    client: HttpClient,
    calendar: Calendar,
    icalEntry: IcalEntry,
    credentials: Credentials?,
    fileManager: FileManager? = null
): PutResourceResult {

    val href = Url(calendar.url.toString().trimEnd('/')+"/"+icalEntry.uid+".ics")

    client.put(href) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        contentType(ContentType.parse("text/calendar").withCharset(Charsets.UTF_8))
        setBody(serializeVCalendar(icalEntry, fileManager))
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
    icalEntry: IcalEntry,
    credentials: Credentials?
): DeleteResourceResult {

    val href = Url(calendar.url.toString().trimEnd('/')+"/"+icalEntry.uid+".ics")

    client.delete(href) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
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
    icalEntry: IcalEntry,
    credentials: Credentials?,
    fileManager: FileManager?
): GetResourceResult {

    val href = Url(calendar.url.toString().trimEnd('/')+"/"+icalEntry.uid+".ics")

    client.get(href) {
        if (credentials != null) {
            basicAuth(credentials.username, credentials.password)
        }
        headers.append(HttpHeaders.IfNoneMatch, icalEntry.etag?:"*")
        contentType(ContentType.parse("text/calendar").withCharset(Charsets.UTF_8))
    }.let { response ->
        if(response.status.isSuccess()) {
            val remoteIcalEntry = parseIcalEntries(response.bodyAsText(), fileManager).firstOrNull() ?: return GetResourceResult.Failed(response.status, "An unknown error occurred.", "${response.status.description} ${response.status.value}")
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

suspend fun uploadFileMultiplatform(
    client: HttpClient,
    targetUrl: Url,
    bytes: ByteArray,
    mimeType: String?,
    credentials: Credentials?
): HttpStatusCode {
    val response = client.put(targetUrl) {
        credentials?.let { basicAuth(it.username, it.password) }
        contentType(mimeType?.let { ContentType.parse(it) } ?: ContentType.Application.OctetStream)
        setBody(bytes)
    }
    return response.status
}

suspend fun downloadFileMultiplatform(
    client: HttpClient,
    sourceUrl: Url,
    credentials: Credentials?
): ByteArray? {
    val response = client.get(sourceUrl) {
        credentials?.let { basicAuth(it.username, it.password) }
    }
    return if (response.status.isSuccess()) response.body<ByteArray>() else null    // TODO: respond with an actual HttpStatusCode
}

suspend fun deleteFileMultiplatform(
    client: HttpClient,
    targetUrl: Url,
    credentials: Credentials?
): HttpStatusCode {
    val response = client.delete(targetUrl) {
        credentials?.let { basicAuth(it.username, it.password) }
    }
    return response.status
}
