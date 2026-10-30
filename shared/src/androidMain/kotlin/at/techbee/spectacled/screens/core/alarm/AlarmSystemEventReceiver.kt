package at.techbee.spectacled.screens.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * AlarmManager forgets everything on reboot, and a clock or time zone change can move alarm
 * times - all of them are answered by a reconcile.
 */
class AlarmSystemEventReceiver : BroadcastReceiver(), KoinComponent {

    private val alarmCoordinator: AlarmCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Napier.d("Reconciling alarms after $action")
                when (action) {
                    Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_TIME_CHANGED -> alarmCoordinator.onTimeOrTimeZoneChanged()
                    else -> alarmCoordinator.reconcile()   // BOOT_COMPLETED, MY_PACKAGE_REPLACED
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
