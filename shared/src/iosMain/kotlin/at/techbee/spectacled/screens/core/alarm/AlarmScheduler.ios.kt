package at.techbee.spectacled.screens.core.alarm

import io.github.aakira.napier.Napier
import platform.Foundation.NSUserDefaults
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNNotificationTrigger
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Remembers which alarms were handed to the notification center, and for when. */
private const val DEFAULTS_KEY_SCHEDULED = "alarm_scheduler_scheduled"

/**
 * How long a scheduled alarm stays on record after its time. It has to outlive the missed-alarm
 * pass of [AlarmCoordinator.reconcile], which looks back [AlarmCoordinator.MISSED_ALARM_GRACE_PERIOD].
 */
private val FIRED_RECORD_RETENTION = AlarmCoordinator.MISSED_ALARM_GRACE_PERIOD + 1.days

/**
 * Hands alarms to UNUserNotificationCenter, which shows them at their time without any app code
 * running. Each request is identified by [ScheduledAlarmKey.asString], so a re-scheduled alarm
 * replaces its earlier request and [dismiss] can address the delivered notification.
 *
 * Unlike Android, iOS delivers the notification itself, so the app never learns that an alarm went
 * off. The next reconcile would see it as missed and show it a second time. To prevent that, every
 * scheduled alarm is recorded with its trigger time, and [showNow] skips an alarm that the system
 * has already delivered at that time.
 */
actual class PlatformAlarmScheduler : AlarmScheduler {

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual override fun replaceAll(alarms: List<ScheduledAlarm>) {
        val now = Clock.System.now()
        val previous = readRecord()
        val newKeys = alarms.map { it.key.asString() }.toSet()

        // Requests added without permission are rejected by iOS, so nothing is recorded as
        // scheduled then. Once permission is given, IosNotifications reconciles again.
        val authorized = IosNotifications.isAuthorized()

        // Only pending requests are removed; delivered notifications stay until dismissed.
        // Without permission everything recorded goes: the record forgets those alarms below, so
        // requests left behind could never be found again and would fire once notifications are
        // allowed - even for entries deleted or completed meanwhile.
        val stale = if (authorized) previous.keys - newKeys else previous.keys
        if (stale.isNotEmpty())
            center.removePendingNotificationRequestsWithIdentifiers(stale.toList())
        if (authorized) {
            alarms.forEach { alarm ->
                val seconds = (alarm.triggerAt - now).inWholeMilliseconds / 1000.0
                add(alarm, UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(seconds.coerceAtLeast(1.0), repeats = false))
            }
        }

        // Keep the alarms that already went off, for showNow, and add the newly scheduled ones.
        val record = previous.filterValues { it <= now && it > now - FIRED_RECORD_RETENTION } +
                (if (authorized) alarms.associate { it.key.asString() to it.triggerAt } else emptyMap())
        writeRecord(record)
        Napier.d("UNUserNotificationCenter: ${if (authorized) alarms.size else 0} alarms scheduled, ${stale.size} removed")
    }

    actual override fun showNow(alarm: ScheduledAlarm): Boolean {
        // Already delivered by the system at its time: nothing more to show.
        val scheduledAt = readRecord()[alarm.key.asString()]
        if (scheduledAt == alarm.triggerAt && scheduledAt <= Clock.System.now())
            return true

        if (!IosNotifications.isAuthorized()) {
            Napier.w("Notifications not allowed, alarm for entry ${alarm.icalEntryId} not shown")
            return false
        }
        add(alarm, trigger = null)   // no trigger: delivered right away
        return true
    }

    actual override fun dismiss(key: ScheduledAlarmKey) {
        center.removeDeliveredNotificationsWithIdentifiers(listOf(key.asString()))
    }

    private fun add(alarm: ScheduledAlarm, trigger: UNNotificationTrigger?) {
        // Title: the entry's summary, or "Alarm" without one. Body: the entry's description, if any.
        val content = UNMutableNotificationContent().apply {
            setTitle(alarm.entrySummary?.takeIf { it.isNotBlank() } ?: IosNotifications.defaultTitle)
            alarm.entryDescription?.takeIf { it.isNotBlank() }?.let { setBody(it) }
            setSound(UNNotificationSound.defaultSound)
            setThreadIdentifier(alarm.icalEntryId.toString())
            // The buttons: "Mark done" only where the task can be changed, "Snooze" always.
            setCategoryIdentifier(if (alarm.entryEditable) CATEGORY_ALARM else CATEGORY_ALARM_READ_ONLY)
            setUserInfo(
                mapOf(
                    USER_INFO_CALENDAR_ID to alarm.calendarId.toString(),
                    USER_INFO_ENTRY_ID to alarm.icalEntryId.toString(),
                    USER_INFO_ALARM_KEY to alarm.key.alarmKey
                )
            )
        }
        val request = UNNotificationRequest.requestWithIdentifier(alarm.key.asString(), content, trigger)
        center.addNotificationRequest(request) { error ->
            error?.let { Napier.w("Alarm for entry ${alarm.icalEntryId} not scheduled: ${it.localizedDescription}") }
        }
    }

    /** Request identifier → trigger time, stored as "epochMillis|identifier" strings. */
    private fun readRecord(): Map<String, Instant> =
        defaults.stringArrayForKey(DEFAULTS_KEY_SCHEDULED).orEmpty()
            .mapNotNull { entry ->
                val (millis, key) = (entry as? String)?.split('|', limit = 2)?.takeIf { it.size == 2 } ?: return@mapNotNull null
                millis.toLongOrNull()?.let { key to Instant.fromEpochMilliseconds(it) }
            }
            .toMap()

    private fun writeRecord(record: Map<String, Instant>) {
        defaults.setObject(record.map { (key, at) -> "${at.toEpochMilliseconds()}|$key" }, forKey = DEFAULTS_KEY_SCHEDULED)
    }
}
