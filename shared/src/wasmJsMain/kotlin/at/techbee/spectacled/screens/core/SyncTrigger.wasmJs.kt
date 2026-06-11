package at.techbee.spectacled.screens.core

import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import io.ktor.client.HttpClient
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


actual class PlatformSyncTrigger : SyncTrigger, KoinComponent {

    private val credentialStore: PlatformCredentialStore by inject()
    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val client: HttpClient by inject()
    private val scope = MainScope()


    actual override fun requestImmediate() {
        scope.launch {
            val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
            SyncCoordinator.syncAllPrincipals(database, credentialStore, client)
        }
    }

    actual override fun requestImmediate(calendarIds: List<Long>) {
        scope.launch {
            val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
            SyncCoordinator.syncSpecificCalendars(calendarIds, database, credentialStore, client)
        }
    }

    actual override fun requestImmediatePush(calendarId: Long) {
        scope.launch {
            val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
            SyncCoordinator.pushLocalChanges(calendarId, database, credentialStore, client)
        }
    }

    actual override fun schedulePeriodic() {
        window.addEventListener(
            type = "focus",
            callback = {
                requestImmediate()
            }
        )
    }

    actual override fun cancel() { /* nothing to cancel */ }

    actual override fun triggerWidgetUpdate() { /* no widgets on web */ }
}
