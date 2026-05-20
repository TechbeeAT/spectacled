package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeZoneForSecondsFromGMT
import platform.Foundation.timeZoneWithName
import kotlin.time.Clock

actual class PlatformInstantFormatter actual constructor(val icsDateTime: IcsDateTime, val deviceZone: TimeZone) : InstantFormatter {

    private val effectiveZone = icsDateTime.effectiveZone()

    private fun TimeZone.toNSTimeZone(): NSTimeZone =
        NSTimeZone.timeZoneWithName(this.id)
            ?: NSTimeZone.timeZoneForSecondsFromGMT(
                this.offsetAt(Clock.System.now()).totalSeconds.toLong()
            )

    private val nsTimeZone = effectiveZone.toNSTimeZone()


    private val dateTimeFormatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
        locale = NSLocale.currentLocale
        timeZone = nsTimeZone
    }

    private val dateFormatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        locale = NSLocale.currentLocale
        timeZone = nsTimeZone
    }

    private val timeFormatter = NSDateFormatter().apply {
        timeStyle = NSDateFormatterShortStyle
        locale = NSLocale.currentLocale
        timeZone = nsTimeZone
    }

    private fun format(formatter: NSDateFormatter): String {

        return if (icsDateTime.isDateOnly) {
            // Extract pure calendar date in UTC
            val localDate = icsDateTime.instant.toLocalDateTime(TimeZone.UTC).date
            val instantAtMidnight = localDate.atStartOfDayIn(effectiveZone)
            formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(instantAtMidnight.epochSeconds.toDouble()))
        } else {
            formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(icsDateTime.instant.epochSeconds.toDouble()))
        }
    }

    actual override fun formatLocalizedDateTime() = format(dateTimeFormatter)
    actual override fun formatLocalizedDate() = format(dateFormatter)
    actual override fun formatLocalizedTime() = format(timeFormatter)
}