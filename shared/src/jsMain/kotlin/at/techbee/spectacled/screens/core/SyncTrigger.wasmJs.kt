package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


actual class PlatformSyncTrigger : SyncTrigger, KoinComponent {

    private val credentialStore: PlatformCredentialStore by inject()
    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val scope = MainScope()


    actual override fun requestImmediate() {
        scope.launch {
            SyncCoordinator.syncAllPrincipals(databaseDriverFactory, credentialStore)
        }
    }

    actual override fun requestImmediate(calendarIds: List<Long>) {
        scope.launch {
            SyncCoordinator.syncSpecificCalendars(calendarIds, databaseDriverFactory, credentialStore)
        }
    }

    actual override fun requestImmediatePush(calendarId: Long) {
        scope.launch {
            SyncCoordinator.pushLocalChanges(calendarId, databaseDriverFactory, credentialStore)
        }
    }

    actual override fun schedulePeriodic() {
        window.addEventListener("focus") {
            requestImmediate()
        }
    }

    actual override fun cancel() { /* nothing to cancel */ }
}
