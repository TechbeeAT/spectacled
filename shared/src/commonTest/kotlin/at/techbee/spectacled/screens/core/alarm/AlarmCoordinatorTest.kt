package at.techbee.spectacled.screens.core.alarm

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.UpcomingAlarm
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class AlarmCoordinatorTest {

    private val now = Instant.parse("2026-10-22T12:00:00Z")
    private val fixedClock = object : Clock { override fun now() = this@AlarmCoordinatorTest.now }

    private fun coordinator(repository: IcalEntryRepository, scheduler: AlarmScheduler) =
        AlarmCoordinator(repository, scheduler, fixedClock, dispatcher = Dispatchers.Unconfined)

    private fun upcoming(alarmId: Long, triggerAt: Instant, entryId: Long = 1L, uid: String? = "alarm-$alarmId") = UpcomingAlarm(
        alarmId = alarmId,
        icalEntryId = entryId,
        calendarId = 7L,
        entryUid = "entry-$entryId",
        entrySummary = "Task $entryId",
        entryDescription = null,
        alarm = Alarm(uid = uid, trigger = AlarmTrigger.Relative(IcsDuration()), description = "Reminder $alarmId"),
        triggerAt = triggerAt
    )

    @Test
    fun reconcile_showsMissedAlarmsWithinGraceAndMarksAllOfThem() = runTest {
        val justMissed = upcoming(1L, now - 10.minutes)
        val missedYesterday = upcoming(2L, now - 23.hours)
        val tooOld = upcoming(3L, now - 2.days)
        val repository = FakeIcalEntryRepository(alarms = listOf(justMissed, missedYesterday, tooOld))
        val scheduler = FakeAlarmScheduler()

        coordinator(repository, scheduler).reconcile()

        assertEquals(listOf(2L, 1L), scheduler.shown.map { it.alarmId }, "earliest first")
        assertEquals(setOf(1L, 2L, 3L), repository.notified.keys, "every missed alarm is marked, shown or not")
        assertTrue(repository.notified.values.all { it == now })
    }

    @Test
    fun reconcile_replacesPendingAlarmsWithTheUpcomingOnes() = runTest {
        val inAnHour = upcoming(10L, now + 1.hours)
        val inAWeek = upcoming(11L, now + 7.days, entryId = 2L)
        val beyondHorizon = upcoming(12L, now + 45.days)
        val repository = FakeIcalEntryRepository(alarms = listOf(inAnHour, inAWeek, beyondHorizon))
        val scheduler = FakeAlarmScheduler()

        coordinator(repository, scheduler).reconcile()

        assertEquals(1, scheduler.replaceAllCalls.size)
        assertEquals(
            listOf(inAnHour.toScheduledAlarm(), inAWeek.toScheduledAlarm()),
            scheduler.replaceAllCalls.single()
        )
        assertTrue(scheduler.shown.isEmpty())
        assertTrue(repository.notified.isEmpty())

        val scheduled = scheduler.replaceAllCalls.single().first()
        assertEquals(ScheduledAlarmKey(1L, "alarm-10"), scheduled.key)
        assertEquals("Task 1", scheduled.entrySummary)
        assertEquals("Reminder 10", scheduled.alarmDescription)
        assertEquals(7L, scheduled.calendarId)
    }

    @Test
    fun reconcile_capsTheNumberOfScheduledAlarms() = runTest {
        val many = (1..60).map { upcoming(it.toLong(), now + it.minutes) }
        val repository = FakeIcalEntryRepository(alarms = many)
        val scheduler = FakeAlarmScheduler()

        coordinator(repository, scheduler).reconcile()

        assertEquals(AlarmCoordinator.MAX_SCHEDULED_ALARMS.toInt(), scheduler.replaceAllCalls.single().size)
        assertEquals((1..50).map { it.toLong() }, scheduler.replaceAllCalls.single().map { it.alarmId }, "the earliest ones")
    }

    @Test
    fun onEntriesDoneOrDeleted_dismissesShownNotificationsThenReconciles() = runTest {
        val withUid = Alarm(uid = "alarm-a", trigger = AlarmTrigger.Relative(IcsDuration()))
        val withoutUid = Alarm(uid = null, trigger = AlarmTrigger.Relative(IcsDuration(isNegative = true, minutes = 5)))
        val entry = IcalEntry.newTask().copy(id = 5L, alarms = listOf(withUid, withoutUid))
        val repository = FakeIcalEntryRepository(entries = listOf(entry))
        val scheduler = FakeAlarmScheduler()

        coordinator(repository, scheduler).onEntriesDoneOrDeleted(listOf(5L, 999L))

        assertEquals(
            listOf(ScheduledAlarmKey(5L, "alarm-a"), ScheduledAlarmKey(5L, withoutUid.schedulingKey())),
            scheduler.dismissed
        )
        assertEquals(1, scheduler.replaceAllCalls.size, "an unknown entry id is skipped, the reconcile still runs")
    }

    @Test
    fun onTimeOrTimeZoneChanged_recomputesBeforeReconciling() = runTest {
        val repository = FakeIcalEntryRepository(alarms = listOf(upcoming(1L, now + 1.hours)))
        val scheduler = FakeAlarmScheduler()

        coordinator(repository, scheduler).onTimeOrTimeZoneChanged()

        assertEquals(1, repository.recomputeCalls)
        assertEquals(1, scheduler.replaceAllCalls.size)
    }

    @Test
    fun onAlarmFired_showsAndMarksTheMatchingAlarm() = runTest {
        val due = upcoming(20L, now, entryId = 3L)
        val repository = FakeIcalEntryRepository(alarms = listOf(due, upcoming(21L, now, entryId = 4L)))
        val scheduler = FakeAlarmScheduler()

        coordinator(repository, scheduler).onAlarmFired(3L, "alarm-20", now)

        assertEquals(listOf(20L), scheduler.shown.map { it.alarmId })
        assertEquals(setOf(20L), repository.notified.keys)
    }

    @Test
    fun onAlarmFired_staysSilentForAnAlarmThatIsGoneOrAlreadyNotified() = runTest {
        val due = upcoming(30L, now)
        val repository = FakeIcalEntryRepository(alarms = listOf(due))
        val scheduler = FakeAlarmScheduler()
        val coordinator = coordinator(repository, scheduler)

        coordinator.onAlarmFired(1L, "alarm-30", now)
        coordinator.onAlarmFired(1L, "alarm-30", now)        // fired again, already notified
        coordinator.onAlarmFired(1L, "deleted-alarm", now)   // no longer exists
        coordinator.onAlarmFired(1L, "alarm-30", now - 1.minutes)  // moved since it was scheduled

        assertEquals(listOf(30L), scheduler.shown.map { it.alarmId })
    }

    @Test
    fun alarmsThatCouldNotBeShownStayUnnotifiedAndAreShownLater() = runTest {
        // Fired a minute ago, the receiver couldn't show it.
        val fired = upcoming(40L, now - 1.minutes)
        val missed = upcoming(41L, now - 1.hours)
        val repository = FakeIcalEntryRepository(alarms = listOf(fired, missed))
        val scheduler = FakeAlarmScheduler().apply { canShow = false }
        val coordinator = coordinator(repository, scheduler)

        coordinator.onAlarmFired(1L, "alarm-40", now - 1.minutes)
        coordinator.reconcile()
        assertTrue(repository.notified.isEmpty(), "nothing was shown, so nothing may be marked")

        scheduler.canShow = true      // e.g. the user granted the permission
        coordinator.reconcile()
        assertEquals(setOf(40L, 41L), repository.notified.keys)
        assertEquals(setOf(40L, 41L), scheduler.shown.map { it.alarmId }.toSet())
    }

    @Test
    fun snooze_dismissesTheNotificationAndSchedulesTheAlarmAgainLater() = runTest {
        val fired = upcoming(50L, now - 1.minutes, entryId = 5L)
        val repository = FakeIcalEntryRepository(alarms = listOf(fired))
        val scheduler = FakeAlarmScheduler()
        val coordinator = coordinator(repository, scheduler)
        coordinator.onAlarmFired(5L, "alarm-50", now - 1.minutes)

        coordinator.snooze(5L, "alarm-50")

        assertEquals(listOf(ScheduledAlarmKey(5L, "alarm-50")), scheduler.dismissed)
        assertEquals(now + AlarmCoordinator.SNOOZE_DURATION, repository.snoozed[50L])
        val rescheduled = scheduler.replaceAllCalls.last().single()
        assertEquals(50L, rescheduled.alarmId)
        assertEquals(now + AlarmCoordinator.SNOOZE_DURATION, rescheduled.triggerAt, "due at the snooze time")
        assertEquals(listOf(50L), scheduler.shown.map { it.alarmId }, "not shown again right away")
    }

    @Test
    fun snooze_ofAnAlarmThatNoLongerExistsOnlyDismisses() = runTest {
        val repository = FakeIcalEntryRepository()
        val scheduler = FakeAlarmScheduler()

        coordinator(repository, scheduler).snooze(5L, "gone")

        assertEquals(listOf(ScheduledAlarmKey(5L, "gone")), scheduler.dismissed)
        assertTrue(repository.snoozed.isEmpty())
        assertEquals(listOf(emptyList<ScheduledAlarm>()), scheduler.replaceAllCalls, "still reconciled")
    }

    @Test
    fun schedulingKey_isTheUidOrStableForEqualBlocks() {
        val alarm = Alarm(uid = null, trigger = AlarmTrigger.Relative(IcsDuration(isNegative = true, minutes = 15)), description = "x")

        assertEquals("my-uid", alarm.copy(uid = "my-uid").schedulingKey())
        assertEquals(alarm.schedulingKey(), alarm.copy().schedulingKey())
        assertNotEquals(alarm.schedulingKey(), alarm.copy(description = "y").schedulingKey())
    }
}

/** Shared with other tests of alarm handling, e.g. SetTaskDoneUseCaseTest. */
internal class FakeAlarmScheduler : AlarmScheduler {
    val replaceAllCalls = mutableListOf<List<ScheduledAlarm>>()
    val shown = mutableListOf<ScheduledAlarm>()
    val dismissed = mutableListOf<ScheduledAlarmKey>()

    override fun replaceAll(alarms: List<ScheduledAlarm>) { replaceAllCalls += alarms }
    /** False simulates a platform that can't show notifications, e.g. without the permission. */
    var canShow = true
    override fun showNow(alarm: ScheduledAlarm): Boolean {
        if (canShow) shown += alarm
        return canShow
    }
    override fun dismiss(key: ScheduledAlarmKey) { dismissed += key }
}

internal class FakeIcalEntryRepository(
    private val alarms: List<UpcomingAlarm> = emptyList(),
    private val entries: List<IcalEntry> = emptyList()
) : IcalEntryRepository {

    val notified = mutableMapOf<Long, Instant>()
    val snoozed = mutableMapOf<Long, Instant>()

    /** Like the real query: a snoozed alarm is due at its snooze time. */
    private val dueAlarms get() = alarms.map { alarm -> snoozed[alarm.alarmId]?.let { alarm.copy(triggerAt = it) } ?: alarm }

    override suspend fun getUpcomingAlarms(from: Instant, until: Instant, limit: Long): List<UpcomingAlarm> =
        dueAlarms
            .filter { it.alarmId !in notified && it.triggerAt >= from && it.triggerAt < until }
            .sortedBy { it.triggerAt }
            .take(limit.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

    override suspend fun markAlarmNotified(alarmId: Long, notifiedAt: Instant) { notified[alarmId] = notifiedAt }

    override suspend fun snoozeAlarm(icalEntryId: Long, alarmKey: String, until: Instant): Boolean {
        val alarm = alarms.firstOrNull { it.icalEntryId == icalEntryId && it.alarm.schedulingKey() == alarmKey } ?: return false
        snoozed[alarm.alarmId] = until
        notified.remove(alarm.alarmId)
        return true
    }

    override suspend fun markAlarmsNotifiedBefore(before: Instant, notifiedAt: Instant) {
        dueAlarms.filter { it.alarmId !in notified && it.triggerAt < before }.forEach { notified[it.alarmId] = notifiedAt }
    }

    override suspend fun getIcalEntryById(id: Long): IcalEntry? = entries.find { it.id == id }

    data class ProgressUpdate(val id: Long, val percentComplete: Long, val status: Status?, val syncState: SyncState)
    val progressUpdates = mutableListOf<ProgressUpdate>()
    override suspend fun updateProgress(id: Long, percentComplete: Long, status: Status?, lastModified: IcsDateTime?, syncState: SyncState) {
        progressUpdates += ProgressUpdate(id, percentComplete, status, syncState)
    }

    var recomputeCalls = 0
    override suspend fun recomputeAlarmTriggerTimes() { recomputeCalls++ }

    // --- not exercised by the alarm handling ---
    override fun getIcalEntriesByCalendarFlow(calendarId: Long): Flow<List<IcalEntry>> = TODO()
    override fun getIcalEntryByUidFlow(calendarId: Long, uid: String): Flow<IcalEntry?> = TODO()
    override fun getAllColors(): Flow<List<Color>> = TODO()
    override fun getAllCategories(): Flow<List<String>> = TODO()
    override fun getLastUsedTimezones(): Flow<List<String>> = TODO()
    override fun getSubtasksByParentUid(calendarId: Long, parentUid: String): Flow<List<IcalEntry>> = TODO()
    override suspend fun getIcalEntryByUid(calendarId: Long, uid: String): IcalEntry? = TODO()
    override suspend fun getIcalEntryByHref(href: Url): IcalEntry? = TODO()
    override suspend fun getDirtyIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> = TODO()
    override suspend fun getIcalEntriesByHrefs(hrefs: List<Url>): List<IcalEntry> = TODO()
    override suspend fun getDeletedDeltaHrefs(calendarId: Long, allServerHrefs: List<Url>): List<Url> = TODO()
    override suspend fun getIcalEntriesByCalendar(calendarId: Long): List<IcalEntry> = TODO()
    override suspend fun insertOrUpdateIcalEntry(icalEntry: IcalEntry): IcalEntry = TODO()
    override suspend fun markAsDeleted(ids: List<Long>) = TODO()
    override suspend fun getIcalEntriesWithSubtasks(icalEntryIds: List<Long>): List<IcalEntry> = TODO()
    override suspend fun moveIcalEntriesToCalendar(icalEntryIds: List<Long>, targetCalendarId: Long) = TODO()
    override suspend fun copyIcalEntriesToCalendar(icalEntryIds: List<Long>, targetCalendarId: Long, sameCalendarSummarySuffix: String): Map<Long, Long> = TODO()
    override suspend fun updateOrderNo(sortedIcalEntryIds: List<Long>) = TODO()
    override suspend fun updateColor(id: Long, color: Color?, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun updateCategory(id: Long, categories: List<String>, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun deleteTrashed(cutoffDateTime: IcsDateTime) = TODO()
    override suspend fun updateSyncMetadata(etag: String?, href: Url?, syncState: SyncState?, id: Long) = TODO()
    override suspend fun insertOrUpdateAttachment(attachment: Attachment) = TODO()
    override suspend fun deleteAttachment(id: Long) = TODO()
    override suspend fun getAttachmentsForEntry(entryId: Long): List<Attachment> = TODO()
}
