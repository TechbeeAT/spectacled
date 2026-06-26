package at.techbee.spectacled.screens.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.widget.SpectacledWidget
import io.ktor.client.HttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

const val CALDAV_PERIODIC_SYNC_WORKER = "caldav-periodic-sync"
const val CALDAV_IMMEDIATE_SYNC_WORKER = "caldav-immediate-sync"
const val CALDAV_IMMEDIATE_SYNC_PUSH_WORKER = "caldav-immediate-sync-push"
const val CALDAV_IMMEDIATE_SYNC_ALL_WORKER = "caldav-immediate-sync-all"
const val WORKER_TARGET_CALENDAR_IDS = "TARGET_CALENDAR_IDS"
const val WORKER_PUSH_ONLY = "PUSH_ONLY"

actual class PlatformSyncTrigger(val context: Context) : SyncTrigger {

    actual override fun requestImmediate() {
        WorkManager.getInstance(context).enqueueUniqueWork(CALDAV_IMMEDIATE_SYNC_ALL_WORKER, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<SyncWorker>().build())
    }

    actual override fun requestImmediate(calendarIds: List<Long>) {
        val inputData = Data.Builder()
            .putLongArray(
                WORKER_TARGET_CALENDAR_IDS,
                calendarIds.toLongArray()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(CALDAV_IMMEDIATE_SYNC_WORKER, ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequestBuilder<SyncWorker>().setInputData(inputData).build())
    }

    actual override fun requestImmediatePush(calendarId: Long) {
        val inputData = Data.Builder()
            .putLongArray(
                WORKER_TARGET_CALENDAR_IDS,
                longArrayOf(calendarId)
            )
            .putBoolean(WORKER_PUSH_ONLY, true)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(CALDAV_IMMEDIATE_SYNC_PUSH_WORKER, ExistingWorkPolicy.APPEND_OR_REPLACE, OneTimeWorkRequestBuilder<SyncWorker>().setInputData(inputData).build())
    }

    actual override fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(CALDAV_PERIODIC_SYNC_WORKER, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    actual override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(CALDAV_PERIODIC_SYNC_WORKER)
    }

    actual override fun triggerWidgetUpdate() {
        SpectacledWidget.updateAll(context)
    }
}

class SyncWorker(val appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params), KoinComponent {
    private val credentialStore: PlatformCredentialStore by inject()
    private val client: HttpClient by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val icalEntryRepository: IcalEntryRepository by inject()

    override suspend fun doWork(): Result {

        val targetCalendarIds = inputData.getLongArray(WORKER_TARGET_CALENDAR_IDS)?.toList()
        val pushOnly = inputData.getBoolean(WORKER_PUSH_ONLY, false)

        if (targetCalendarIds.isNullOrEmpty()) {
            // Default behavior: Sync everything
            SyncCoordinator.syncAllPrincipals(calendarRepository, icalEntryRepository, credentialStore, client)
        } else if(pushOnly && targetCalendarIds.size == 1) {
            SyncCoordinator.pushLocalChanges(targetCalendarIds.first(), calendarRepository, icalEntryRepository, credentialStore, client)
        } else {
            SyncCoordinator.syncSpecificCalendars(
                targetCalendarIds,
                calendarRepository,
                icalEntryRepository,
                credentialStore,
                client
            )
        }

        SpectacledWidget.updateAll(appContext)

        return Result.success()
    }
}
