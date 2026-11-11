package at.techbee.spectacled.screens.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import at.techbee.spectacled.screens.core.SetTaskDoneUseCase
import at.techbee.spectacled.screens.core.SyncTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Instant

/**
 * Woken by AlarmManager at an alarm's trigger time, and by the action buttons of the notification
 * it shows. The intents only identify the alarm; what to do is decided by [AlarmCoordinator].
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_ALARM = "at.techbee.spectacled.action.ALARM"
        const val ACTION_MARK_DONE = "at.techbee.spectacled.action.ALARM_MARK_DONE"
        const val ACTION_SNOOZE = "at.techbee.spectacled.action.ALARM_SNOOZE"
        const val EXTRA_ENTRY_ID = "entryId"
        const val EXTRA_ALARM_KEY = "alarmKey"
        const val EXTRA_TRIGGER_AT = "triggerAt"
    }

    private val alarmCoordinator: AlarmCoordinator by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    private val setTaskDone: SetTaskDoneUseCase by inject()
    private val syncTrigger: SyncTrigger by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(ACTION_ALARM, ACTION_MARK_DONE, ACTION_SNOOZE)) return
        val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L).takeIf { it != -1L } ?: return
        val alarmKey = intent.getStringExtra(EXTRA_ALARM_KEY) ?: return
        val triggerAt = Instant.fromEpochMilliseconds(intent.getLongExtra(EXTRA_TRIGGER_AT, 0L))

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_ALARM -> alarmCoordinator.onAlarmFired(entryId, alarmKey, triggerAt)
                    ACTION_SNOOZE -> alarmCoordinator.snooze(entryId, alarmKey)
                    ACTION_MARK_DONE -> {
                        // Done dismisses the entry's notifications. If it can't be done (deleted
                        // meanwhile, read-only), at least take this one away - the button did nothing.
                        if (setTaskDone.updateProgress(entryId, done = true))
                            syncTrigger.triggerWidgetUpdate()
                        else
                            alarmScheduler.dismiss(ScheduledAlarmKey(entryId, alarmKey))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
