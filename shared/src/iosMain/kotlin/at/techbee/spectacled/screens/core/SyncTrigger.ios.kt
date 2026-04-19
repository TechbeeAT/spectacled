package at.techbee.spectacled.screens.core

import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


actual class PlatformSyncTrigger : SyncTrigger {
    actual override fun requestImmediate() {
        Napier.d("PlatformSyncTrigger: Immediate sync requested")
        IOSSyncEntryPoint.runBackgroundSync {
            Napier.d("PlatformSyncTrigger: Immediate sync finished")  // callback is mainly for iOS code, not relevant here
        }
    }

    actual override fun requestImmediate(calendarIds: List<Long>) {
        Napier.d("PlatformSyncTrigger: Immediate sync for specific calendars requested")
        IOSSyncEntryPoint.runBackgroundSyncForSpecificCalendars(calendarIds) {
            Napier.d("PlatformSyncTrigger: Immediate sync for specific calendars finished")  // callback is mainly for iOS code, not relevant here
        }
    }

    actual override fun requestImmediatePush(calendarId: Long) {
        Napier.d("PlatformSyncTrigger: Immediate push for specific calendar requested")
        IOSSyncEntryPoint.runBackgroundPushForSpecificCalendar(calendarId) {
            Napier.d("PlatformSyncTrigger: Immediate push for specific calendar finished")  // callback is mainly for iOS code, not relevant here
        }    }

    actual override fun schedulePeriodic() { /* handled directly in swift code in iOSApp.swift & AppDelegate.swift */ }
    actual override fun cancel() { /* nothing to cancel in iOS */ }
}


object IOSSyncEntryPoint : KoinComponent {

    private val credentialStore: PlatformCredentialStore by inject()
    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val bgScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun runBackgroundSync(onFinished: () -> Unit) {
        bgScope.launch {
            val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
            try {
                Napier.d("BG Sync coroutine started")
                SyncCoordinator.syncAllPrincipals(database, credentialStore)
                Napier.d("BG Sync coroutine finished")
            } finally {
                onFinished()
            }
        }
    }

    fun runBackgroundSyncForSpecificCalendars(calendarIds: List<Long>, onFinished: () -> Unit) {
        bgScope.launch {
            val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
            try {
                Napier.d("BG Sync for specific calendars coroutine started")
                SyncCoordinator.syncSpecificCalendars(calendarIds, database, credentialStore)
                Napier.d("BG Sync for specific calendars coroutine finished")
            } finally {
                onFinished()
            }
        }
    }

    fun runBackgroundPushForSpecificCalendar(calendarId: Long, onFinished: () -> Unit) {
        bgScope.launch {
            val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
            try {
                Napier.d("BG Push for specific calendar coroutine started")
                SyncCoordinator.pushLocalChanges(calendarId, database, credentialStore)
                Napier.d("BG Push for specific calendar coroutine finished")
            } finally {
                onFinished()
            }
        }
    }

    fun cancelBackgroundSync() {
        bgScope.cancel()
    }
}