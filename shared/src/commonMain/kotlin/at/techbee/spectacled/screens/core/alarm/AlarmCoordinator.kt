package at.techbee.spectacled.screens.core.alarm

import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.UpcomingAlarm
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.core.mapper.dto.toIcsBlock
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Keeps the platform's scheduled alarms in line with the database.
 *
 * [reconcile] is the single entry point and is called whenever alarms may have changed: after a
 * local save, after a sync, at app start and from platform hooks such as boot. It doesn't diff:
 * it reads the next alarms and hands the whole list to the [AlarmScheduler], which is simpler
 * than tracking individual changes and heals any state the platform lost.
 *
 * Trigger times are computed when an entry is saved (IcalEntryRepositoryImpl.saveAlarms), so a
 * reconcile alone doesn't move all-day or floating alarms after a time zone change - that is
 * what [onTimeOrTimeZoneChanged] is for.
 */
class AlarmCoordinator(
    private val icalEntryRepository: IcalEntryRepository,
    private val alarmScheduler: AlarmScheduler,
    private val clock: Clock = Clock.System,
    /**
     * Where the work runs. Callers are mostly on the main thread (a ViewModel after each save),
     * and the platform calls are not cheap - on Android up to 50 AlarmManager IPCs per reconcile.
     */
    private val dispatcher: CoroutineDispatcher = ioDispatcher
) {

    companion object {
        /**
         * How far ahead alarms are handed to the platform. Anything later is picked up by one of
         * the next reconciles (the periodic sync runs far more often than this).
         */
        val SCHEDULING_HORIZON: Duration = 30.days

        /** Stays below the 64 pending notification requests iOS keeps per app. */
        const val MAX_SCHEDULED_ALARMS = 50L

        /**
         * An alarm whose time passed while the app couldn't fire it (device off, app closed on
         * desktop, arrived via sync after its time) is still shown, up to this long after its
         * time. Older ones are only marked, so a long-untouched task doesn't flood the user.
         */
        val MISSED_ALARM_GRACE_PERIOD: Duration = 1.days

        /** How long the notification's snooze action puts an alarm off. */
        val SNOOZE_DURATION: Duration = 10.minutes
    }

    private val reconcileMutex = Mutex()

    suspend fun reconcile() = withContext(dispatcher) { reconcileMutex.withLock {
        val now = clock.now()

        // Alarms that were missed: show them late rather than never, within the grace period ...
        val missed = icalEntryRepository.getUpcomingAlarms(
            from = now - MISSED_ALARM_GRACE_PERIOD,
            until = now,
            limit = Long.MAX_VALUE
        )
        missed.forEach { upcoming ->
            // Not shown (e.g. no permission yet): leave it, a later reconcile can still show it.
            if (alarmScheduler.showNow(upcoming.toScheduledAlarm()))
                icalEntryRepository.markAlarmNotified(upcoming.alarmId, now)
        }
        // ... and write off anything older, so it is neither shown nor queried again.
        icalEntryRepository.markAlarmsNotifiedBefore(before = now - MISSED_ALARM_GRACE_PERIOD, notifiedAt = now)

        val upcoming = icalEntryRepository.getUpcomingAlarms(
            from = now,
            until = now + SCHEDULING_HORIZON,
            limit = MAX_SCHEDULED_ALARMS
        )
        alarmScheduler.replaceAll(upcoming.map { it.toScheduledAlarm() })
        Napier.d("Alarms reconciled: ${missed.size} missed, ${upcoming.size} scheduled")
    } }

    /**
     * A platform alarm registered by [AlarmScheduler.replaceAll] went off. The alarm is looked up
     * fresh, so one that was deleted, moved, completed or already notified since it was scheduled
     * stays silent. Runs under the same lock as [reconcile], so a reconcile at the same moment
     * can't show the same alarm a second time as "missed".
     */
    suspend fun onAlarmFired(icalEntryId: Long, alarmKey: String, triggerAt: Instant) = withContext(dispatcher) { reconcileMutex.withLock {
        val alarm = icalEntryRepository
            .getUpcomingAlarms(from = triggerAt, until = triggerAt + 1.milliseconds, limit = Long.MAX_VALUE)
            .firstOrNull { it.icalEntryId == icalEntryId && it.alarm.schedulingKey() == alarmKey }

        if (alarm == null) {
            Napier.d("Alarm $icalEntryId/$alarmKey fired but is no longer due, skipping")
        } else if (alarmScheduler.showNow(alarm.toScheduledAlarm())) {
            icalEntryRepository.markAlarmNotified(alarm.alarmId, clock.now())
        }
        // Not shown: the next reconcile picks it up as missed, within the grace period.
    } }

    /**
     * The user snoozed the notification of [alarmKey]: it is removed now and shown again after
     * [SNOOZE_DURATION]. The snooze is stored with the alarm, so it survives reconciles and reboots
     * like any other alarm; editing the alarm's time drops it. Runs under the same lock as
     * [reconcile], so one already running can't show the alarm again and mark it notified in
     * between, which would lose the snooze.
     */
    suspend fun snooze(icalEntryId: Long, alarmKey: String) {
        withContext(dispatcher) { reconcileMutex.withLock {
            alarmScheduler.dismiss(ScheduledAlarmKey(icalEntryId, alarmKey))
            if (!icalEntryRepository.snoozeAlarm(icalEntryId, alarmKey, until = clock.now() + SNOOZE_DURATION))
                Napier.d("Alarm $icalEntryId/$alarmKey to snooze no longer exists")
        } }
        reconcile()
    }

    /** The device's clock or time zone changed: stored trigger times may be off, recompute first. */
    suspend fun onTimeOrTimeZoneChanged() {
        icalEntryRepository.recomputeAlarmTriggerTimes()
        reconcile()
    }

    /**
     * Called when entries were completed or deleted: their notifications, if shown, are stale.
     * [reconcile] alone wouldn't remove them, it only touches pending alarms.
     */
    suspend fun onEntriesDoneOrDeleted(icalEntryIds: List<Long>) = withContext(dispatcher) {
        icalEntryIds.forEach { icalEntryId ->
            icalEntryRepository.getIcalEntryById(icalEntryId)?.alarms?.forEach { alarm ->
                alarmScheduler.dismiss(ScheduledAlarmKey(icalEntryId, alarm.schedulingKey()))
            }
        }
        reconcile()
    }
}

/** The alarm's part of a [ScheduledAlarmKey]: its UID, or a hash of the whole block if it has none. */
fun Alarm.schedulingKey(): String = uid ?: toIcsBlock().hashCode().toUInt().toString(16)

fun UpcomingAlarm.toScheduledAlarm() = ScheduledAlarm(
    key = ScheduledAlarmKey(icalEntryId, alarm.schedulingKey()),
    alarmId = alarmId,
    icalEntryId = icalEntryId,
    calendarId = calendarId,
    entryUid = entryUid,
    entrySummary = entrySummary,
    entryDescription = entryDescription,
    entryEditable = entryEditable,
    alarmDescription = alarm.description,
    triggerAt = triggerAt
)
