package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.grouping_due_30_plus_days
import spectacled.shared.generated.resources.grouping_due_within_x_days
import spectacled.shared.generated.resources.grouping_due_within_x_hours
import spectacled.shared.generated.resources.grouping_last_x_days
import spectacled.shared.generated.resources.grouping_last_x_hours
import spectacled.shared.generated.resources.grouping_last_year
import spectacled.shared.generated.resources.grouping_older
import spectacled.shared.generated.resources.grouping_overdue
import spectacled.shared.generated.resources.grouping_start_in_30_plus_days
import spectacled.shared.generated.resources.grouping_start_in_past
import spectacled.shared.generated.resources.grouping_start_within_x_days
import spectacled.shared.generated.resources.grouping_start_within_x_hours
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

enum class ListGrouping(
    val lowerThreshold: Duration?,
    val upperThreshold: Duration?,
    val stringRes: StringResource?,
    val stringResParam: Int?

) {
    GROUP_24_HOURS(
        (-24).hours,
        0.hours,
        Res.string.grouping_last_x_hours,
        24
    ),
    GROUP_48_HOURS(
        (-48).hours,
        (-24).hours,
        Res.string.grouping_last_x_hours,
        48
    ),
    GROUP_7_DAYS(
        (-7).days,
        (-48).hours,
        Res.string.grouping_last_x_days,
        7
    ),
    GROUP_30_DAYS(
        (-30).days,
        (-7).days,
        Res.string.grouping_last_x_days,
        30
    ),
    GROUP_YEAR(
        (-365).days,
        (-30).days,
        Res.string.grouping_last_year,
        null
    ),
    GROUP_OLDER(
        Duration.INFINITE.unaryMinus(),
        (-365).days,
        Res.string.grouping_older,
        null
    ),

    /* DUE */
    DUE_OVERDUE(
        null,
        0.minutes,
        Res.string.grouping_overdue,
        null
    ),
    DUE_WITHIN_24_HOURS(
        0.minutes,
        24.hours,
        Res.string.grouping_due_within_x_hours,
        24
    ),
    DUE_WITHIN_48_HOURS(
        24.hours,
        48.hours,
        Res.string.grouping_due_within_x_hours,
        48
    ),
    DUE_WITHIN_7_DAYS(
        48.hours,
        7.days,
        Res.string.grouping_due_within_x_days,
        7
    ),
    DUE_WITHIN_30_DAYS(
        7.days,
        30.days,
        Res.string.grouping_due_within_x_days,
        30
    ),
    DUE_IN_30_PLUS_DAYS(
        31.days,
        null,
        Res.string.grouping_due_30_plus_days,
        null
    ),

    /* START */
    START_IN_PAST(
        null,
        0.minutes,
        Res.string.grouping_start_in_past,
        null
    ),
    START_WITHIN_24_HOURS(
        0.minutes,
        24.hours,
        Res.string.grouping_start_within_x_hours,
        24
    ),
    START_WITHIN_48_HOURS(
        24.hours,
        48.hours,
        Res.string.grouping_start_within_x_hours,
        48
    ),
    START_WITHIN_7_DAYS(
        48.hours,
        7.days,
        Res.string.grouping_start_within_x_days,
        7
    ),
    START_WITHIN_30_DAYS(
        7.days,
        30.days,
        Res.string.grouping_start_within_x_days,
        30
    ),
    START_IN_30_PLUS_DAYS(
        31.days,
        null,
        Res.string.grouping_start_in_30_plus_days,
        null
    );


    companion object {

        val createdGroups = setOf(GROUP_24_HOURS, GROUP_48_HOURS, GROUP_7_DAYS, GROUP_30_DAYS, GROUP_YEAR, GROUP_OLDER)
        val lastModifiedGroups = setOf(GROUP_24_HOURS, GROUP_48_HOURS, GROUP_7_DAYS, GROUP_30_DAYS, GROUP_YEAR, GROUP_OLDER)
        val dueGroups = setOf(DUE_OVERDUE, DUE_WITHIN_24_HOURS, DUE_WITHIN_48_HOURS, DUE_WITHIN_7_DAYS, DUE_WITHIN_30_DAYS, DUE_IN_30_PLUS_DAYS)
        val startGroups = setOf(START_IN_PAST, START_WITHIN_24_HOURS, START_WITHIN_48_HOURS, START_WITHIN_7_DAYS, START_WITHIN_30_DAYS, START_IN_30_PLUS_DAYS)

        /**
         * The date bucket [icsDateTime] falls into, or `null` when there is no date or it matches no
         * bucket. A `null` result routes the entry to the list's dedicated "no criteria" section
         * instead of being forced into a catch-all bucket.
         */
        fun getGrouping(baseGroups: Set<ListGrouping>, icsDateTime: IcsDateTime?): ListGrouping? {
            if (icsDateTime == null) return null
            return baseGroups.find { group ->
                val lowerIcsDateTime = IcsDateTime(group.lowerThreshold?.let { Clock.System.now().plus(it) } ?: Instant.DISTANT_PAST, false).toLocalDateTime()
                val upperIcsDateTime = IcsDateTime(group.upperThreshold?.let { Clock.System.now().plus(it) } ?: Instant.DISTANT_FUTURE, false).toLocalDateTime()
                icsDateTime.toLocalDateTime() in lowerIcsDateTime..upperIcsDateTime
            }
        }
    }
}
