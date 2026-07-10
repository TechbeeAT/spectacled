package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import io.ktor.client.HttpClient
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


actual class PlatformSyncTrigger : SyncTrigger, KoinComponent {

    private val credentialStore: PlatformCredentialStore by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val icalEntryRepository: IcalEntryRepository by inject()
    private val fileManager: PlatformFileManager by inject()
    private val client: HttpClient by inject()
    private val scope = MainScope()


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