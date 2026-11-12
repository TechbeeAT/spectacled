package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class ListGroupingTest {

    private val losAngeles = TimeZone.of("America/Los_Angeles")

    // Nov 12, 17:00 in Los Angeles (UTC-8) is already Nov 13, 01:00 UTC.
    private val now = Instant.parse("2026-11-13T01:00:00Z")

    private fun grouping(groups: Set<ListGrouping>, icsDateTime: IcsDateTime?) =
        ListGrouping.getGrouping(groups, icsDateTime, losAngeles, now)

    /** A floating value with the given wall-clock time (floating values store it as UTC). */
    private fun floating(localDateTime: LocalDateTime) =
        IcsDateTime(localDateTime.toInstant(TimeZone.UTC), isDateOnly = false, timeZone = null)

    @Test
    fun noDate_hasNoGroup() {
        assertNull(grouping(ListGrouping.dueGroups, null))
    }

    @Test
    fun zonedValues_areComparedAsPointsInTime() {
        val in10Hours = IcsDateTime(now.plus(10.hours), isDateOnly = false, timeZone = losAngeles)
        val in30Hours = IcsDateTime(now.plus(30.hours), isDateOnly = false, timeZone = TimeZone.UTC)
        val oneHourAgo = IcsDateTime(now.minus(1.hours), isDateOnly = false, timeZone = losAngeles)

        assertEquals(ListGrouping.DUE_WITHIN_24_HOURS, grouping(ListGrouping.dueGroups, in10Hours))
        assertEquals(ListGrouping.DUE_WITHIN_48_HOURS, grouping(ListGrouping.dueGroups, in30Hours))
        assertEquals(ListGrouping.DUE_OVERDUE, grouping(ListGrouping.dueGroups, oneHourAgo))
    }

    @Test
    fun floatingValues_areResolvedInTheDeviceZone() {
        // 20:00 local is 3 hours ahead, 15:00 local is 2 hours ago. Read as UTC wall clock instead,
        // 20:00 would be in the past and 15:00 would be overdue too - both must follow local time.
        assertEquals(
            ListGrouping.DUE_WITHIN_24_HOURS,
            grouping(ListGrouping.dueGroups, floating(LocalDateTime(2026, 11, 12, 20, 0)))
        )
        assertEquals(
            ListGrouping.DUE_OVERDUE,
            grouping(ListGrouping.dueGroups, floating(LocalDateTime(2026, 11, 12, 15, 0)))
        )
        // Nov 13, 16:00 local is 23 hours ahead, still within 24 hours.
        assertEquals(
            ListGrouping.START_WITHIN_24_HOURS,
            grouping(ListGrouping.startGroups, floating(LocalDateTime(2026, 11, 13, 16, 0)))
        )
    }

    @Test
    fun dateOnlyValues_startAtLocalMidnight() {
        // Nov 13 starts at 00:00 local = 7 hours from now. Read as UTC midnight it would be 1 hour ago.
        val tomorrow = IcsDateTime(LocalDate(2026, 11, 13).atStartOfDayIn(TimeZone.UTC), isDateOnly = true)
        assertEquals(ListGrouping.DUE_WITHIN_24_HOURS, grouping(ListGrouping.dueGroups, tomorrow))
    }

    @Test
    fun utcTimestamps_landInTheRecentGroups() {
        val modified2HoursAgo = IcsDateTime(now.minus(2.hours), isDateOnly = false, timeZone = TimeZone.UTC)
        val modified30HoursAgo = IcsDateTime(now.minus(30.hours), isDateOnly = false, timeZone = TimeZone.UTC)

        assertEquals(ListGrouping.GROUP_24_HOURS, grouping(ListGrouping.lastModifiedGroups, modified2HoursAgo))
        assertEquals(ListGrouping.GROUP_48_HOURS, grouping(ListGrouping.lastModifiedGroups, modified30HoursAgo))
    }
}
