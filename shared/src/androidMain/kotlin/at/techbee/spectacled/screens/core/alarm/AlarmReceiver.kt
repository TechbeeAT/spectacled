package at.techbee.spectacled.screens.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Instant

/**
 * Woken by AlarmManager at an alarm's trigger time. The intent only identifies the alarm, what
 * to show is decided by [AlarmCoordinator.onAlarmFired].
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_ALARM = "at.techbee.spectacled.action.ALARM"
        const val EXTRA_ENTRY_ID = "entryId"
        const val EXTRA_ALARM_KEY = "alarmKey"
        const val EXTRA_TRIGGER_AT = "triggerAt"
    }

    private val alarmCoordinator: AlarmCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM) return
        val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L).takeIf { it != -1L } ?: return
        val alarmKey = intent.getStringExtra(EXTRA_ALARM_KEY) ?: return
        val triggerAt = Instant.fromEpochMilliseconds(intent.getLongExtra(EXTRA_TRIGGER_AT, 0L))

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                alarmCoordinator.onAlarmFired(entryId, alarmKey, triggerAt)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
