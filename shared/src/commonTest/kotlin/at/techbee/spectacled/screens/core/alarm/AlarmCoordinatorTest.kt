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

    private fun upcoming(alarmId: Long, triggerAt: Instant, entryId: Long = 1L, uid: String? = "alarm-$alarmId") = UpcomingAlarm(
        alarmId = alarmId,
        icalEntryId = entryId,
        calendarId = 7L,
        entryUid = "entry-$entryId",
        entrySummary = "Task $entryId",
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

        AlarmCoordinator(repository, scheduler, fixedClock).reconcile()

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

        AlarmCoordinator(repository, scheduler, fixedClock).reconcile()

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

        AlarmCoordinator(repository, scheduler, fixedClock).reconcile()

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

        AlarmCoordinator(repository, scheduler, fixedClock).onEntriesDoneOrDeleted(listOf(5L, 999L))

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

        AlarmCoordinator(repository, scheduler, fixedClock).onTimeOrTimeZoneChanged()

        assertEquals(1, repository.recomputeCalls)
        assertEquals(1, scheduler.replaceAllCalls.size)
    }

    @Test
    fun schedulingKey_isTheUidOrStableForEqualBlocks() {
        val alarm = Alarm(uid = null, trigger = AlarmTrigger.Relative(IcsDuration(isNegative = true, minutes = 15)), description = "x")

        assertEquals("my-uid", alarm.copy(uid = "my-uid").schedulingKey())
        assertEquals(alarm.schedulingKey(), alarm.copy().schedulingKey())
        assertNotEquals(alarm.schedulingKey(), alarm.copy(description = "y").schedulingKey())
    }
}

private class FakeAlarmScheduler : AlarmScheduler {
    val replaceAllCalls = mutableListOf<List<ScheduledAlarm>>()
    val shown = mutableListOf<ScheduledAlarm>()
    val dismissed = mutableListOf<ScheduledAlarmKey>()

    override suspend fun requestPermissionIfNeeded() = true
    override fun replaceAll(alarms: List<ScheduledAlarm>) { replaceAllCalls += alarms }
    override fun showNow(alarm: ScheduledAlarm) { shown += alarm }
    override fun dismiss(key: ScheduledAlarmKey) { dismissed += key }
}

private class FakeIcalEntryRepository(
    private val alarms: List<UpcomingAlarm> = emptyList(),
    private val entries: List<IcalEntry> = emptyList()
) : IcalEntryRepository {

    val notified = mutableMapOf<Long, Instant>()

    override suspend fun getUpcomingAlarms(from: Instant, until: Instant, limit: Long): List<UpcomingAlarm> =
        alarms
            .filter { it.alarmId !in notified && it.triggerAt >= from && it.triggerAt < until }
            .sortedBy { it.triggerAt }
            .take(limit.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())

    override suspend fun markAlarmNotified(alarmId: Long, notifiedAt: Instant) { notified[alarmId] = notifiedAt }

    override suspend fun markAlarmsNotifiedBefore(before: Instant, notifiedAt: Instant) {
        alarms.filter { it.alarmId !in notified && it.triggerAt < before }.forEach { notified[it.alarmId] = notifiedAt }
    }

    override suspend fun getIcalEntryById(id: Long): IcalEntry? = entries.find { it.id == id }

    var recomputeCalls = 0
    override suspend fun recomputeAlarmTriggerTimes() { recomputeCalls++ }

    // --- not exercised by the alarm coordinator ---
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
    override suspend fun updateProgress(id: Long, percentComplete: Long, status: Status?, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun updateOrderNo(sortedIcalEntryIds: List<Long>) = TODO()
    override suspend fun updateColor(id: Long, color: Color?, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun updateCategory(id: Long, categories: List<String>, lastModified: IcsDateTime?, syncState: SyncState) = TODO()
    override suspend fun deleteTrashed(cutoffDateTime: IcsDateTime) = TODO()
    override suspend fun updateSyncMetadata(etag: String?, href: Url?, syncState: SyncState?, id: Long) = TODO()
    override suspend fun insertOrUpdateAttachment(attachment: Attachment) = TODO()
    override suspend fun deleteAttachment(id: Long) = TODO()
    override suspend fun getAttachmentsForEntry(entryId: Long): List<Attachment> = TODO()
}
