package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.ioDispatcher
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlinx.coroutines.withContext

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

/**
 * Every call dispatches its own network work onto [ioDispatcher], so callers (ViewModels) can
 * launch on the Main dispatcher without thinking about threading — the same contract the
 * repositories already follow. This is also load-bearing on Compose Desktop: the Main dispatcher
 * there doesn't reliably resume suspended Ktor network continuations (nor fire the HttpTimeout
 * timer), so a discovery/sync call left on Main hangs forever without ever timing out. Running on
 * IO resumes reliably on every platform.
 */
class DefaultWebDavRemoteCalendarDataSource(
    private val client: HttpClient,
) : WebDavRemoteCalendarDataSource {

    override suspend fun discoverPrincipals(location: Url, credentials: Credentials?) =
        withContext(ioDispatcher) { discoverPrincipalsMultiplatform(client, location, credentials) }

    override suspend fun discoverHomeCollections(principal: Principal, credentials: Credentials?) =
        withContext(ioDispatcher) { discoverHomeCollectionsMultiplatform(client, principal, credentials) }

    override suspend fun discoverCalendars(homeCollection: HomeCollection, credentials: Credentials?) =
        withContext(ioDispatcher) { discoverCalendarsMultiplatform(client, homeCollection, credentials) }

    override suspend fun createCalendar(calendar: Calendar, credentials: Credentials?) =
        withContext(ioDispatcher) { createCalendarMultiplatform(client, calendar, credentials) }

    override suspend fun updateCalendar(calendar: Calendar, credentials: Credentials?) =
        withContext(ioDispatcher) { updateCalDavCalendarMultiplatform(client, calendar, credentials) }

    override suspend fun deleteCalendar(calendar: Calendar, credentials: Credentials?) =
        withContext(ioDispatcher) { deleteCalendarMultiplatform(client, calendar, credentials) }
}
