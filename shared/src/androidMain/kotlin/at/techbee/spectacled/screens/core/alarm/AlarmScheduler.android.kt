package at.techbee.spectacled.screens.core.alarm

import android.content.Context

/** Step 1: no-op. AlarmManager + notification receivers follow in the next step. */
actual class PlatformAlarmScheduler(private val context: Context) : AlarmScheduler {

    actual override suspend fun requestPermissionIfNeeded(): Boolean = false
    actual override fun replaceAll(alarms: List<ScheduledAlarm>) { /* not implemented yet */ }
    actual override fun showNow(alarm: ScheduledAlarm) { /* not implemented yet */ }
    actual override fun dismiss(key: ScheduledAlarmKey) { /* not implemented yet */ }
}
