package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-format tests for the low-level `*Multiplatform` DAV functions. These exercise the seam
 * that the fake-interface [at.techbee.spectacled.screens.core.SyncCoordinatorTest] deliberately
 * skips: the parsing of real CalDAV multistatus XML into domain results, plus the HTTP status-code
 * mapping and per-call credential handling. A Ktor [MockEngine] scripts canned server responses so
 * the parser can be driven with realistic Nextcloud/Radicale-style payloads without a network.
 */
class WebDavParsingTest {

    // --- test infrastructure ------------------------------------------------------------------

    /** Captures the single request the function under test issues, for request-shape assertions. */
    private class RequestCapture {
        var request: HttpRequestData? = null
    }

    /** A client whose one handler always answers with [body] and [status]. */
    private fun mockClient(
        status: HttpStatusCode = HttpStatusCode.MultiStatus,
        body: String = "",
        capture: RequestCapture? = null,
    ): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                capture?.request = request
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/xml; charset=utf-8"),
                )
            }
        }
    }

    private val credentials = Credentials(
        server = Url("https://dav.example.com"),
        username = "alice",
        password = "s3cret",
    )

    private fun calendar(url: String = "https://dav.example.com/cal/personal/") = Calendar(
        id = 1L,
        homeCollectionId = 1L,
        url = Url(url),
        displayName = "Personal",
        calendarDescription = null,
        color = null,
        ctag = null,
        supportedComponents = listOf(CalendarComponent.VJOURNAL),
        calDavPrivileges = emptyList(),
        calendarSyncStatus = null,
        syncToken = "sync-token-1",
    )

    // --- syncCollectionMultiplatform ----------------------------------------------------------

    @Test
    fun syncCollection_parsesSyncTokenAndEtagMap() = runTest {
        // entry1 changed (200 + etag), deleted.ics removed (a non-200 propstat -> null etag).
        val body = """
            <?xml version="1.0" encoding="utf-8"?>
            <d:multistatus xmlns:d="DAV:">
              <d:sync-token>http://sync.example.com/token/2</d:sync-token>
              <d:response>
                <d:href>/cal/personal/entry1.ics</d:href>
                <d:propstat>
                  <d:prop><d:getetag>"etag-1"</d:getetag></d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/cal/personal/deleted.ics</d:href>
                <d:propstat>
                  <d:prop/>
                  <d:status>HTTP/1.1 404 Not Found</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val result = syncCollectionMultiplatform(mockClient(body = body), calendar(), credentials)

        val success = assertIs<MultigetSyncCollectionResult.Success>(result)
        assertEquals("http://sync.example.com/token/2", success.syncToken)
        assertEquals(2, success.hrefs.size)
        val changed = success.hrefs.entries.single { it.key.toString().endsWith("entry1.ics") }
        val removed = success.hrefs.entries.single { it.key.toString().endsWith("deleted.ics") }
        assertEquals("\"etag-1\"", changed.value)
        assertNull(removed.value, "a deleted resource must map to a null etag (the deletion signal)")
    }

    @Test
    fun syncCollection_resolvesRelativeHrefAgainstCalendarUrl() = runTest {
        val body = """
            <d:multistatus xmlns:d="DAV:">
              <d:sync-token>t</d:sync-token>
              <d:response>
                <d:href>/cal/personal/entry1.ics</d:href>
                <d:propstat><d:prop><d:getetag>"e"</d:getetag></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val result = syncCollectionMultiplatform(mockClient(body = body), calendar(), credentials)

        val success = assertIs<MultigetSyncCollectionResult.Success>(result)
        assertEquals(
            "https://dav.example.com/cal/personal/entry1.ics",
            success.hrefs.keys.single().toString(),
        )
    }

    @Test
    fun syncCollection_sendsReportWithDepthAndCredentials() = runTest {
        val capture = RequestCapture()
        val body = """<d:multistatus xmlns:d="DAV:"><d:sync-token>t</d:sync-token></d:multistatus>"""

        syncCollectionMultiplatform(mockClient(body = body, capture = capture), calendar(), credentials)

        val request = capture.request!!
        assertEquals("REPORT", request.method.value)
        assertEquals("1", request.headers[HttpHeaders.Depth])
        assertTrue(
            request.headers[HttpHeaders.Authorization]?.startsWith("Basic ") == true,
            "credentials passed per call must produce a Basic auth header",
        )
    }

    @Test
    fun syncCollection_withoutCredentialsSendsNoAuthHeader() = runTest {
        val capture = RequestCapture()
        val body = """<d:multistatus xmlns:d="DAV:"><d:sync-token>t</d:sync-token></d:multistatus>"""

        syncCollectionMultiplatform(mockClient(body = body, capture = capture), calendar(), credentials = null)

        assertNull(capture.request!!.headers[HttpHeaders.Authorization])
    }

    @Test
    fun syncCollection_maps404ToNotFound() = runTest {
        val result = syncCollectionMultiplatform(
            mockClient(status = HttpStatusCode.NotFound), calendar(), credentials,
        )
        assertIs<MultigetSyncCollectionResult.NotFound>(result)
    }

    @Test
    fun syncCollection_mapsUnauthorizedToNotAuthorized() = runTest {
        val result = syncCollectionMultiplatform(
            mockClient(status = HttpStatusCode.Unauthorized), calendar(), credentials,
        )
        assertIs<MultigetSyncCollectionResult.NotAuthorized>(result)
    }

    @Test
    fun syncCollection_mapsServerErrorToFailed() = runTest {
        val result = syncCollectionMultiplatform(
            mockClient(status = HttpStatusCode.InternalServerError), calendar(), credentials,
        )
        val failed = assertIs<MultigetSyncCollectionResult.Failed>(result)
        assertEquals(HttpStatusCode.InternalServerError, failed.status)
    }

    // --- multigetResourceHrefsMultiplatform ---------------------------------------------------

    @Test
    fun multigetHrefs_includesOnlyOkPropstats() = runTest {
        val body = """
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/cal/personal/a.ics</d:href>
                <d:propstat><d:prop><d:getetag>"e-a"</d:getetag></d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat>
              </d:response>
              <d:response>
                <d:href>/cal/personal/forbidden.ics</d:href>
                <d:propstat><d:prop/><d:status>HTTP/1.1 403 Forbidden</d:status></d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val result = multigetResourceHrefsMultiplatform(mockClient(body = body), calendar(), credentials)

        val success = assertIs<MultigetResourceHrefETagResult.Success>(result)
        assertEquals(1, success.hrefs.size, "a non-200 propstat must be excluded from the multiget map")
        val only = success.hrefs.entries.single()
        assertTrue(only.key.toString().endsWith("a.ics"))
        assertEquals("\"e-a\"", only.value)
    }

    @Test
    fun multigetHrefs_mapsUnauthorizedToNotAuthorized() = runTest {
        val result = multigetResourceHrefsMultiplatform(
            mockClient(status = HttpStatusCode.Forbidden), calendar(), credentials,
        )
        assertIs<MultigetResourceHrefETagResult.NotAuthorized>(result)
    }

    // --- discoverPrincipalsMultiplatform ------------------------------------------------------

    @Test
    fun discoverPrincipals_extractsAndResolvesPrincipalHref() = runTest {
        val body = """
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:current-user-principal><d:href>/principals/users/alice/</d:href></d:current-user-principal>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val result = discoverPrincipalsMultiplatform(
            mockClient(body = body), Url("https://dav.example.com/"), credentials,
        )

        val success = assertIs<DiscoverPrincipalsResult.Success>(result)
        assertEquals(1, success.principals.size)
        assertEquals(
            "https://dav.example.com/principals/users/alice/",
            success.principals.single().principalUrl.toString(),
        )
    }

    @Test
    fun discoverPrincipals_allForbiddenPropstatsBecomeNotAuthorized() = runTest {
        // A 207 at the HTTP layer, but every inner propstat is 403: must be read as "not authorized".
        val body = """
            <d:multistatus xmlns:d="DAV:">
              <d:response>
                <d:href>/</d:href>
                <d:propstat><d:prop/><d:status>HTTP/1.1 403 Forbidden</d:status></d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val result = discoverPrincipalsMultiplatform(
            mockClient(body = body), Url("https://dav.example.com/"), credentials,
        )

        assertIs<DiscoverPrincipalsResult.NotAuthorized>(result)
    }

    // --- discoverHomeCollectionsMultiplatform -------------------------------------------------

    @Test
    fun discoverHomeCollections_extractsHomeSetDisplayNameAndAddressSet() = runTest {
        val body = """
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              <d:response>
                <d:href>/principals/users/alice/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>Alice</d:displayname>
                    <cal:calendar-user-address-set><d:href>mailto:alice@example.com</d:href></cal:calendar-user-address-set>
                    <cal:calendar-home-set><d:href>/cal/</d:href></cal:calendar-home-set>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val principal = Principal(
            id = 1L,
            principalUrl = Url("https://dav.example.com/principals/users/alice/"),
            displayName = null,
            calendarUserAddressSet = emptyList(),
        )

        val result = discoverHomeCollectionsMultiplatform(mockClient(body = body), principal, credentials)

        val success = assertIs<DiscoverHomeCollectionsResult.Success>(result)
        assertEquals("Alice", success.principalDisplayName)
        assertEquals(listOf("mailto:alice@example.com"), success.principalCalendarUserAddressSet)
        assertEquals(1, success.homeCollections.size)
        assertEquals(
            "https://dav.example.com/cal/",
            success.homeCollections.single().url.toString(),
        )
    }

    // --- discoverCalendarsMultiplatform -------------------------------------------------------

    @Test
    fun discoverCalendars_keepsSupportedCalendarsAndExtractsHomeCollectionPrivileges() = runTest {
        // Three responses: the home collection itself (privileges only, not a calendar),
        // a VJOURNAL calendar (kept), and a VEVENT-only calendar (dropped - unsupported component).
        val body = """
            <d:multistatus xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav"
                           xmlns:cs="http://calendarserver.org/ns/" xmlns:a="http://apple.com/ns/ical/">
              <d:response>
                <d:href>/cal/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:resourcetype><d:collection/></d:resourcetype>
                    <d:current-user-privilege-set>
                      <d:privilege><d:bind/></d:privilege>
                      <d:privilege><d:unbind/></d:privilege>
                    </d:current-user-privilege-set>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/cal/journal/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>My Journal</d:displayname>
                    <cal:calendar-description>Diary</cal:calendar-description>
                    <cs:getctag>ctag-99</cs:getctag>
                    <a:calendar-color>#FF3366CC</a:calendar-color>
                    <d:resourcetype><d:collection/><cal:calendar/></d:resourcetype>
                    <cal:supported-calendar-component-set><cal:comp name="VJOURNAL"/></cal:supported-calendar-component-set>
                    <d:current-user-privilege-set>
                      <d:privilege><d:read/></d:privilege>
                      <d:privilege><d:write/></d:privilege>
                    </d:current-user-privilege-set>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
              <d:response>
                <d:href>/cal/events/</d:href>
                <d:propstat>
                  <d:prop>
                    <d:displayname>Events</d:displayname>
                    <d:resourcetype><d:collection/><cal:calendar/></d:resourcetype>
                    <cal:supported-calendar-component-set><cal:comp name="VEVENT"/></cal:supported-calendar-component-set>
                  </d:prop>
                  <d:status>HTTP/1.1 200 OK</d:status>
                </d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val homeCollection = HomeCollection(
            id = 1L,
            principalId = 1L,
            url = Url("https://dav.example.com/cal/"),
            calDavPrivileges = emptyList(),
        )

        val result = discoverCalendarsMultiplatform(mockClient(body = body), homeCollection, credentials)

        val success = assertIs<DiscoverCalendarsResult.Success>(result)

        // Only the VJOURNAL calendar survives filtering.
        assertEquals(1, success.calendars.size)
        val journal = success.calendars.single()
        assertEquals("https://dav.example.com/cal/journal/", journal.url.toString())
        assertEquals("My Journal", journal.displayName)
        assertEquals("Diary", journal.calendarDescription)
        assertEquals("ctag-99", journal.ctag)
        assertTrue(journal.color != null, "calendar-color must be parsed")
        assertTrue(journal.supportedComponents.contains(CalendarComponent.VJOURNAL))
        assertTrue(journal.calDavPrivileges.contains(CalDavPrivilege.READ))
        assertTrue(journal.calDavPrivileges.contains(CalDavPrivilege.WRITE))

        // Home-collection-level privileges come from the response whose href IS the home collection.
        assertTrue(success.calDavPrivileges.contains(CalDavPrivilege.BIND))
        assertTrue(success.calDavPrivileges.contains(CalDavPrivilege.UNBIND))
    }

    @Test
    fun discoverCalendars_maps404ToNotFound() = runTest {
        val homeCollection = HomeCollection(
            id = 1L,
            principalId = 1L,
            url = Url("https://dav.example.com/cal/"),
            calDavPrivileges = emptyList(),
        )
        val result = discoverCalendarsMultiplatform(
            mockClient(status = HttpStatusCode.NotFound), homeCollection, credentials,
        )
        assertIs<DiscoverCalendarsResult.NotFound>(result)
    }
}
