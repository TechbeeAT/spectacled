package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
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
    private val client: HttpClient by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val icalEntryRepository: IcalEntryRepository by inject()
    private val fileManager: PlatformFileManager by inject()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null


    actual override fun requestImmediate() {
        scope.launch {
            SyncCoordinator.syncAllPrincipals(calendarRepository, icalEntryRepository, fileManager, credentialStore, client)
        }
    }

    actual override fun requestImmediate(calendarIds: List<Long>) {
        scope.launch {
            SyncCoordinator.syncSpecificCalendars(calendarIds, calendarRepository, icalEntryRepository, fileManager, credentialStore, client)
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