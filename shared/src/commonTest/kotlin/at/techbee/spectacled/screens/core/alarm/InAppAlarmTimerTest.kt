package at.techbee.spectacled.screens.core.alarm

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class InAppAlarmTimerTest {

    private val start = Instant.parse("2026-11-12T08:00:00Z")

    private fun alarm(id: Long, at: Instant) = ScheduledAlarm(
        key = ScheduledAlarmKey(id, "alarm-$id"),
        alarmId = id,
        icalEntryId = id,
        calendarId = 1L,
        entryUid = "entry-$id",
        entrySummary = null,
        entryDescription = null,
        entryEditable = true,
        alarmDescription = null,
        triggerAt = at
    )

    /** A timer on the test's virtual time; [clockOffset] simulates the wall clock jumping ahead (sleep). */
    private class Fixture(scope: TestScope, start: Instant) {
        val fired = mutableListOf<Long>()
        var clockOffset = Duration.ZERO
        val timer = InAppAlarmTimer(
            scope = scope.backgroundScope,
            now = { start + scope.testScheduler.currentTime.milliseconds + clockOffset },
            onDue = { fired += it.alarmId }
        )
    }

    @Test
    fun firesEachAlarmOnceAtItsTimeInOrder() = runTest {
        val fixture = Fixture(this, start)
        fixture.timer.replaceAll(listOf(alarm(2, start + 10.minutes), alarm(1, start + 5.minutes)))

        advanceTimeBy(5.minutes - 1.seconds)
        assertTrue(fixture.fired.isEmpty())
        advanceTimeBy(1.seconds + 1.milliseconds)
        assertEquals(listOf(1L), fixture.fired)
        advanceTimeBy(1.hours)
        assertEquals(listOf(1L, 2L), fixture.fired)
    }

    @Test
    fun replaceAllDropsWhatIsNoLongerScheduled() = runTest {
        val fixture = Fixture(this, start)
        fixture.timer.replaceAll(listOf(alarm(1, start + 5.minutes), alarm(2, start + 10.minutes)))
        advanceTimeBy(1.minutes)

        fixture.timer.replaceAll(listOf(alarm(2, start + 10.minutes)))
        advanceTimeBy(1.hours)
        assertEquals(listOf(2L), fixture.fired)

        fixture.timer.replaceAll(emptyList())
        advanceTimeBy(1.hours)
        assertEquals(listOf(2L), fixture.fired)
    }

    @Test
    fun aClockJumpIsNoticedWithinTheMaximumSleep() = runTest {
        val fixture = Fixture(this, start)
        fixture.timer.replaceAll(listOf(alarm(1, start + 2.hours)))
        runCurrent()

        // The computer slept: the wall clock is past the alarm, while hardly any time ran here.
        fixture.clockOffset = 3.hours
        advanceTimeBy(30.seconds + 1.milliseconds)
        assertEquals(listOf(1L), fixture.fired)
    }
}
