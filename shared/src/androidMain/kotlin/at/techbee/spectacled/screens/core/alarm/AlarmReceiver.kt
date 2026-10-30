package at.techbee.spectacled.screens.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Woken by AlarmManager at an alarm's trigger time. The intent only identifies the alarm; what
 * to show is looked up fresh, so an alarm that was deleted, moved or already notified since it
 * was scheduled stays silent.
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_ALARM = "at.techbee.spectacled.action.ALARM"
        const val EXTRA_ENTRY_ID = "entryId"
        const val EXTRA_ALARM_KEY = "alarmKey"
        const val EXTRA_TRIGGER_AT = "triggerAt"
    }

    private val icalEntryRepository: IcalEntryRepository by inject()
    private val alarmScheduler: AlarmScheduler by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM) return
        val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L).takeIf { it != -1L } ?: return
        val alarmKey = intent.getStringExtra(EXTRA_ALARM_KEY) ?: return
        val triggerAt = Instant.fromEpochMilliseconds(intent.getLongExtra(EXTRA_TRIGGER_AT, 0L))

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Exactly this alarm, still un-notified and its entry still open: the query already
                // excludes done, cancelled, deleted and already notified alarms.
                val alarm = icalEntryRepository
                    .getUpcomingAlarms(from = triggerAt, until = triggerAt + 1.milliseconds, limit = Long.MAX_VALUE)
                    .firstOrNull { it.icalEntryId == entryId && it.alarm.schedulingKey() == alarmKey }

                if (alarm == null) {
                    Napier.d("Alarm $entryId/$alarmKey fired but is no longer due, skipping")
                } else {
                    alarmScheduler.showNow(alarm.toScheduledAlarm())
                    icalEntryRepository.markAlarmNotified(alarm.alarmId, Clock.System.now())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
