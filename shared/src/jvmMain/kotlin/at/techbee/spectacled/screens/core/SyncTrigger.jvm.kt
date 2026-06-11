package at.techbee.spectacled.screens.core

import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

actual class PlatformSyncTrigger : SyncTrigger, KoinComponent {

    private val credentialStore: PlatformCredentialStore by inject()
    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val client: HttpClient by inject()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null


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
        job = scope.launch {
            while (isActive) {
                delay(15.minutes)
                requestImmediate()
            }
        }
    }

    actual override fun cancel() {
        job?.cancel()
    }

    actual override fun triggerWidgetUpdate() {
        // No widgets on JVM
    }
}