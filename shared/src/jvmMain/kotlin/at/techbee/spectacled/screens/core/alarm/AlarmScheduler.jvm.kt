package at.techbee.spectacled.screens.core.alarm

/** Step 1: no-op. An in-process timer with system tray notifications follows in a later step. */
actual class PlatformAlarmScheduler : AlarmScheduler {

    actual override suspend fun requestPermissionIfNeeded(): Boolean = false
    actual override fun replaceAll(alarms: List<ScheduledAlarm>) { /* not implemented yet */ }
    actual override fun showNow(alarm: ScheduledAlarm) { /* not implemented yet */ }
    actual override fun dismiss(key: ScheduledAlarmKey) { /* not implemented yet */ }
}
