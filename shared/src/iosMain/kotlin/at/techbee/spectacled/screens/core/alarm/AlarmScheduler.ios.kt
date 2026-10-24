package at.techbee.spectacled.screens.core.alarm

/** Step 1: no-op. UNUserNotificationCenter scheduling follows in a later step. */
actual class PlatformAlarmScheduler : AlarmScheduler {

    actual override suspend fun requestPermissionIfNeeded(): Boolean = false
    actual override fun replaceAll(alarms: List<ScheduledAlarm>) { /* not implemented yet */ }
    actual override fun showNow(alarm: ScheduledAlarm) { /* not implemented yet */ }
    actual override fun dismiss(key: ScheduledAlarmKey) { /* not implemented yet */ }
}
