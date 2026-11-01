package at.techbee.spectacled.screens.core.data.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.AlarmTriggerRelation
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Runs the alarm parts of [IcalEntryRepositoryImpl] against a real (in-memory) SQLite database,
 * so the SQL itself - the getUpcomingAlarms filters, the lastNotifiedAt bookkeeping, the trigger
 * time recompute - is covered, not just a fake of it.
 */
class IcalEntryRepositoryAlarmTest {

    private val due = Instant.parse("2026-11-05T09:00:00Z")
    private val fifteenMinutesBeforeDue = AlarmTrigger.Relative(IcsDuration(isNegative = true, minutes = 15), AlarmTriggerRelation.END)
    private val alarmTime = due - 15.minutes

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: IcalEntryRepositoryImpl
    private lateinit var originalTimeZone: java.util.TimeZone

    @BeforeTest
    fun setUp() = runTest {
        // Trigger times of all-day and floating dates depend on the device zone - pin it.
        originalTimeZone = java.util.TimeZone.getDefault()
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))

        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SpectacledDatabase.Schema.synchronous().create(driver).await()
        val database = SpectacledDatabase(driver)
        repository = IcalEntryRepositoryImpl { database }
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        java.util.TimeZone.setDefault(originalTimeZone)
    }

    private fun task(
        uid: String,
        alarms: List<Alarm> = listOf(Alarm(uid = "alarm-$uid", trigger = fifteenMinutesBeforeDue, description = "x")),
        calendarId: Long = 1L
    ) = IcalEntry.newTask().copy(
        uid = uid,
        calendarId = calendarId,
        summary = "Task $uid",
        due = IcsDateTime(due, isDateOnly = false, timeZone = TimeZone.UTC),
        alarms = alarms,
        syncState = SyncState.LOCAL_MODIFIED
    )

    private suspend fun upcoming(from: Instant = alarmTime - 1.days, until: Instant = alarmTime + 1.days, limit: Long = 100) =
        repository.getUpcomingAlarms(from, until, limit)

    @Test
    fun alarmsRoundTripThroughTheDatabase() = runTest {
        val saved = repository.insertOrUpdateIcalEntry(task("a"))

        assertEquals(task("a").alarms, saved.alarms)
        val alarm = upcoming().single()
        assertEquals(saved.id, alarm.icalEntryId)
        assertEquals(alarmTime, alarm.triggerAt)
        assertEquals("Task a", alarm.entrySummary)
    }

    @Test
    fun upcomingAlarms_skipDoneCancelledDeletedRecurringAndNonNotifyingEntries() = runTest {
        repository.insertOrUpdateIcalEntry(task("open"))
        repository.insertOrUpdateIcalEntry(task("done").copy(status = Status.COMPLETED, percentComplete = 100))
        repository.insertOrUpdateIcalEntry(task("cancelled").copy(status = Status.CANCELLED))
        repository.insertOrUpdateIcalEntry(task("recurring").copy(extraProperties = listOf(RawIcsProperty("RRULE", "RRULE:FREQ=DAILY"))))
        repository.insertOrUpdateIcalEntry(
            task("email", alarms = listOf(Alarm(uid = "mail", action = Alarm.ACTION_EMAIL, trigger = fifteenMinutesBeforeDue)))
        )
        val deleted = repository.insertOrUpdateIcalEntry(task("deleted"))
        repository.markAsDeleted(listOf(deleted.id))

        assertEquals(listOf("Task open"), upcoming().map { it.entrySummary })
    }

    @Test
    fun upcomingAlarms_respectWindowOrderAndLimit() = runTest {
        repository.insertOrUpdateIcalEntry(task("late", alarms = listOf(Alarm(uid = "l", trigger = AlarmTrigger.Absolute(alarmTime + 2.hours)))))
        repository.insertOrUpdateIcalEntry(task("early", alarms = listOf(Alarm(uid = "e", trigger = AlarmTrigger.Absolute(alarmTime)))))
        repository.insertOrUpdateIcalEntry(task("outside", alarms = listOf(Alarm(uid = "o", trigger = AlarmTrigger.Absolute(alarmTime + 3.days)))))

        assertEquals(listOf("Task early", "Task late"), upcoming().map { it.entrySummary })
        assertEquals(listOf("Task early"), upcoming(limit = 1).map { it.entrySummary })
        // "until" is exclusive, "from" inclusive
        assertEquals(listOf("Task early"), upcoming(from = alarmTime, until = alarmTime + 2.hours).map { it.entrySummary })
    }

    @Test
    fun notifiedState_survivesASave_butIsResetWhenTheAlarmMoves() = runTest {
        val saved = repository.insertOrUpdateIcalEntry(task("a"))
        repository.markAlarmNotified(upcoming().single().alarmId, alarmTime)
        assertTrue(upcoming().isEmpty())

        // Saving the entry rewrites its alarm rows; the alarm is the same, so it stays notified.
        repository.insertOrUpdateIcalEntry(saved.copy(summary = "Renamed"))
        assertTrue(upcoming().isEmpty())

        // Moving the due date moves the alarm: it has to go off again.
        val movedDue = IcsDateTime(due + 1.hours, isDateOnly = false, timeZone = TimeZone.UTC)
        repository.insertOrUpdateIcalEntry(saved.copy(due = movedDue))
        assertEquals(listOf(alarmTime + 1.hours), upcoming().map { it.triggerAt })
    }

    @Test
    fun markAlarmsNotifiedBefore_writesOffOnlyOlderAlarms() = runTest {
        repository.insertOrUpdateIcalEntry(task("old", alarms = listOf(Alarm(uid = "old", trigger = AlarmTrigger.Absolute(alarmTime - 2.days)))))
        repository.insertOrUpdateIcalEntry(task("new"))

        repository.markAlarmsNotifiedBefore(before = alarmTime - 1.days, notifiedAt = alarmTime)

        assertEquals(listOf("Task new"), upcoming(from = alarmTime - 3.days).map { it.entrySummary })
    }

    @Test
    fun recomputeAlarmTriggerTimes_followsTheDeviceTimeZone() = runTest {
        // An all-day due date: "1 day before" fires at local midnight of the day before.
        val allDayDue = IcsDateTime(LocalDate(2026, 11, 5).atStartOfDayIn(TimeZone.UTC), isDateOnly = true)
        val oneDayBefore = AlarmTrigger.Relative(IcsDuration(isNegative = true, days = 1), AlarmTriggerRelation.END)
        repository.insertOrUpdateIcalEntry(task("a", alarms = listOf(Alarm(uid = "a", trigger = oneDayBefore))).copy(due = allDayDue))
        val wide = suspend { upcoming(from = alarmTime - 7.days, until = alarmTime + 7.days) }
        assertEquals(listOf(LocalDate(2026, 11, 4).atStartOfDayIn(TimeZone.UTC)), wide().map { it.triggerAt })

        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Tokyo"))
        repository.recomputeAlarmTriggerTimes()

        val tokyo = TimeZone.of("Asia/Tokyo")
        assertEquals(listOf(LocalDate(2026, 11, 4).atStartOfDayIn(tokyo)), wide().map { it.triggerAt })
    }

    @Test
    fun moveIcalEntriesToCalendar_keepsTheNotifiedState() = runTest {
        val saved = repository.insertOrUpdateIcalEntry(task("a"))
        repository.markAlarmNotified(upcoming().single().alarmId, alarmTime)

        repository.moveIcalEntriesToCalendar(listOf(saved.id), targetCalendarId = 2L)

        assertEquals(1, repository.getIcalEntryByUid(2L, "a")?.alarms?.size)
        assertTrue(upcoming().isEmpty(), "the copy's alarm already went off, it must not fire again")
    }
}
