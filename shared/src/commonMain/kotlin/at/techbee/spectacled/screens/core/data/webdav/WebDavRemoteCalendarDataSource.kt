package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import io.ktor.client.HttpClient
import io.ktor.http.Url

/**
 * All CalDAV server operations about principals, home collections, and calendars (discovery +
 * calendar management), behind an interface so every DAV call goes through an injectable seam
 * rather than a direct function call. The data source is stateless with respect to credentials -
 * they are passed per call - so [DefaultWebDavRemoteCalendarDataSource] can be a single injected
 * singleton holding only the transport. This is also the natural boundary to lift into a
 * standalone DAV library later.
 */
interface WebDavRemoteCalendarDataSource {
    suspend fun discoverPrincipals(location: Url, credentials: Credentials?): DiscoverPrincipalsResult
    suspend fun discoverHomeCollections(principal: Principal, credentials: Credentials?): DiscoverHomeCollectionsResult
    suspend fun discoverCalendars(homeCollection: HomeCollection, credentials: Credentials?): DiscoverCalendarsResult
    suspend fun createCalendar(calendar: Calendar, credentials: Credentials?): UpsertCalendarResult
    suspend fun updateCalendar(calendar: Calendar, credentials: Credentials?): UpsertCalendarResult
    suspend fun deleteCalendar(calendar: Calendar, credentials: Credentials?): DeleteCalendarResult
}

class DefaultWebDavRemoteCalendarDataSource(
    private val client: HttpClient,
) : WebDavRemoteCalendarDataSource {

    override suspend fun discoverPrincipals(location: Url, credentials: Credentials?) =
        discoverPrincipalsMultiplatform(client, location, credentials)

    override suspend fun discoverHomeCollections(principal: Principal, credentials: Credentials?) =
        discoverHomeCollectionsMultiplatform(client, principal, credentials)

    override suspend fun discoverCalendars(homeCollection: HomeCollection, credentials: Credentials?) =
        discoverCalendarsMultiplatform(client, homeCollection, credentials)

    override suspend fun createCalendar(calendar: Calendar, credentials: Credentials?) =
        createCalendarMultiplatform(client, calendar, credentials)

    override suspend fun updateCalendar(calendar: Calendar, credentials: Credentials?) =
        updateCalDavCalendarMultiplatform(client, calendar, credentials)

    override suspend fun deleteCalendar(calendar: Calendar, credentials: Credentials?) =
        deleteCalendarMultiplatform(client, calendar, credentials)
}
