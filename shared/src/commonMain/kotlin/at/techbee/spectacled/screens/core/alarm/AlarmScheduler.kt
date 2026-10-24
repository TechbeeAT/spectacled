package at.techbee.spectacled.screens.core.alarm

import kotlin.time.Instant

/**
 * Identifies a scheduled alarm across saves. Alarm rows are rewritten on every save of their entry
 * (see IcalEntryRepositoryImpl.saveAlarms), so the row id can't be used; the entry id plus the
 * alarm's UID (or a hash of its block if it has none) is stable instead. Platforms derive their
 * notification ids / request identifiers from it.
 */
data class ScheduledAlarmKey(
    val icalEntryId: Long,
    val alarmKey: String
) {
    /** A single string for platforms that identify requests by name, e.g. UNUserNotificationCenter. */
    fun asString() = "$icalEntryId:$alarmKey"
}

/** Everything a platform needs to schedule and later show the notification for one alarm. */
data class ScheduledAlarm(
    val key: ScheduledAlarmKey,
    /** Row id at scheduling time. Only valid until the entry is saved again - re-query before using it. */
    val alarmId: Long,
    val icalEntryId: Long,
    val calendarId: Long,
    val entryUid: String,
    val entrySummary: String?,
    val alarmDescription: String?,
    val triggerAt: Instant
)

/**
 * Hands alarms over to the platform so they fire without the app running. The platform never
 * decides what to schedule - [AlarmCoordinator] does and passes the complete list each time.
 */
interface AlarmScheduler {

    /** Asks for the notification permission where one is needed. True if notifications may be shown. */
    suspend fun requestPermissionIfNeeded(): Boolean

    /**
     * Replaces every *pending* alarm with [alarms]: anything scheduled before and not in the list
     * is cancelled. Must never remove a notification that was already shown - the user dismisses
     * those, or [dismiss] does once the entry is done or deleted.
     */
    fun replaceAll(alarms: List<ScheduledAlarm>)

    /** Shows the notification for [alarm] right away, used for alarms that were missed. */
    fun showNow(alarm: ScheduledAlarm)

    /** Removes the shown notification for [key], if any. Pending alarms are not affected. */
    fun dismiss(key: ScheduledAlarmKey)
}

expect class PlatformAlarmScheduler : AlarmScheduler {
    override suspend fun requestPermissionIfNeeded(): Boolean
    override fun replaceAll(alarms: List<ScheduledAlarm>)
    override fun showNow(alarm: ScheduledAlarm)
    override fun dismiss(key: ScheduledAlarmKey)
}
