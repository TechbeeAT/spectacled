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
import platform.Foundation.localeIdentifier
import platform.Foundation.timeZoneForSecondsFromGMT
import platform.Foundation.timeZoneWithName
import kotlin.native.concurrent.ThreadLocal
import kotlin.time.Clock

@ThreadLocal
private val formatterCache = mutableMapOf<String, NSDateFormatter>()

actual fun IcsDateTime.formatLocalized(icsDateTimeFormat: IcsDateTimeFormat): String {

    fun TimeZone.toNSTimeZone(): NSTimeZone =
        NSTimeZone.timeZoneWithName(this.id)
            ?: NSTimeZone.timeZoneForSecondsFromGMT(
                this.offsetAt(Clock.System.now()).totalSeconds.toLong()
            )


    val effectiveZone = this.effectiveZone()

    val locale = NSLocale.currentLocale
    val cacheKey = "$icsDateTimeFormat-${locale.localeIdentifier}-${effectiveZone.id}"

    val formatter = formatterCache.getOrPut(cacheKey) {
        when(icsDateTimeFormat) {
            IcsDateTimeFormat.DATE_TIME -> NSDateFormatter().apply {
                dateStyle = NSDateFormatterMediumStyle
                timeStyle = NSDateFormatterShortStyle
            }
            IcsDateTimeFormat.DATE -> NSDateFormatter().apply {
                dateStyle = NSDateFormatterMediumStyle
            }
            IcsDateTimeFormat.TIME -> NSDateFormatter().apply {
                timeStyle = NSDateFormatterShortStyle
            }
            IcsDateTimeFormat.DAY_OF_WEEK_SHORT -> NSDateFormatter().apply {
                dateFormat = "EEE"
            }
            IcsDateTimeFormat.FULL_MONTH_NAME -> NSDateFormatter().apply {
                dateFormat = "MMMM"
            }
        }.apply {
            this.locale = locale
            this.timeZone = effectiveZone.toNSTimeZone() // Your existing helper
        }
    }

    return if (this.isDateOnly) {
        // Extract pure calendar date in UTC
        val localDate = this.instant.toLocalDateTime(TimeZone.UTC).date
        val instantAtMidnight = localDate.atStartOfDayIn(effectiveZone)
        formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(instantAtMidnight.epochSeconds.toDouble()))
    } else {
        formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(this.instant.epochSeconds.toDouble()))
    }
}
