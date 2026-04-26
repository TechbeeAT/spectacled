package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures

import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.grouping_last_x_days
import spectacled.shared.generated.resources.grouping_last_x_hours
import spectacled.shared.generated.resources.grouping_last_year
import spectacled.shared.generated.resources.grouping_older
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class ListGrouping(
    val upperThresholdDays: Duration,
    val lowerThresholdDays: Duration,
    val stringRes: StringResource?,
    val stringResParam: Int?

) {
    GROUP_24_HOURS(
        0.hours,
        24.hours,
        Res.string.grouping_last_x_hours,
        24
    ),
    GROUP_48_HOURS(
        24.hours,
        48.hours,
        Res.string.grouping_last_x_hours,
        48
    ),
    GROUP_7_DAYS(
        48.hours,
        7.days,
        Res.string.grouping_last_x_days,
        7
    ),
    GROUP_30_DAYS(
        7.days,
        30.days,
        Res.string.grouping_last_x_days,
        30
    ),
    GROUP_YEAR(
        30.days,
        365.days,
        Res.string.grouping_last_year,
        null
    ),
    GROUP_OLDER(
        365.days,
        Duration.INFINITE,
        Res.string.grouping_older,
        null
    ),
    GROUP_NONE(
        Duration.INFINITE,
        Duration.INFINITE,
        null,
        null
    );

    companion object {
        @OptIn(ExperimentalTime::class)
        fun getGrouping(timestamp: Instant): ListGrouping {
            return entries.find {
                timestamp in Clock.System.now().minus(it.lowerThresholdDays)..Clock.System.now().minus(it.upperThresholdDays)
            }?: GROUP_NONE

        }
    }
}