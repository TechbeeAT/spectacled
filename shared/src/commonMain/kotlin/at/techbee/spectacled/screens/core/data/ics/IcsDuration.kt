package at.techbee.spectacled.screens.core.data.ics

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * An RFC 5545 §3.3.6 DURATION value, e.g. "-PT15M", "P1D" or "P2W".
 *
 * The components are kept as written instead of being collapsed into a [kotlin.time.Duration]:
 * weeks and days are *nominal* (a day across a DST change is 23 or 25 hours), hours, minutes and
 * seconds are exact. Keeping them apart preserves that meaning and lets a parsed value be written
 * back exactly as it was received.
 */
data class IcsDuration(
    val isNegative: Boolean = false,
    val weeks: Int = 0,
    val days: Int = 0,
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0
) {

    companion object {

        // dur-week, or dur-date and/or dur-time. Component order is fixed, each is optional, at
        // least one must be present (checked below). Producers mixing weeks with other components
        // ("P1W2D") are not RFC 5545 conformant and are rejected - callers keep such a value as-is.
        private val DURATION_REGEX = Regex("""^([+-])?P(?:(\d+)W|(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?)$""")

        fun parse(value: String): IcsDuration? {
            val match = DURATION_REGEX.matchEntire(value.trim().uppercase()) ?: return null
            val (sign, weeks, days, hours, minutes, seconds) = match.destructured
            if (weeks.isEmpty() && days.isEmpty() && hours.isEmpty() && minutes.isEmpty() && seconds.isEmpty())
                return null     // "P" or "PT" alone
            if (value.contains('T', ignoreCase = true) && hours.isEmpty() && minutes.isEmpty() && seconds.isEmpty())
                return null     // "P1DT" - a time designator without a time component
            return runCatching {
                IcsDuration(
                    isNegative = sign == "-",
                    weeks = weeks.ifEmpty { "0" }.toInt(),
                    days = days.ifEmpty { "0" }.toInt(),
                    hours = hours.ifEmpty { "0" }.toInt(),
                    minutes = minutes.ifEmpty { "0" }.toInt(),
                    seconds = seconds.ifEmpty { "0" }.toInt()
                )
            }.getOrNull()   // number overflow
        }
    }

    /** Formats the value in RFC 5545 syntax. A zero duration is written as "PT0S". */
    fun format(): String = buildString {
        if (isNegative) append('-')
        append('P')
        if (weeks != 0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
            append(weeks).append('W')
            return@buildString
        }
        val totalDays = weeks * 7 + days
        if (totalDays != 0) append(totalDays).append('D')
        if (hours != 0 || minutes != 0 || seconds != 0) {
            append('T')
            if (hours != 0) append(hours).append('H')
            // RFC 5545 grammar only allows seconds after minutes when hours are given ("PT1H0M30S").
            if (minutes != 0 || (hours != 0 && seconds != 0)) append(minutes).append('M')
            if (seconds != 0) append(seconds).append('S')
        } else if (totalDays == 0) {
            append("T0S")
        }
    }

    /**
     * Adds this duration to [instant]. Weeks and days are added as calendar days in [timeZone]
     * (so "-P1D" before 09:00 stays at 09:00 across a DST change), the rest as exact time.
     */
    fun addTo(instant: Instant, timeZone: TimeZone): Instant {
        val sign = if (isNegative) -1 else 1
        val nominalDays = weeks * 7 + days
        val withDays = if (nominalDays != 0) instant.plus(sign * nominalDays, DateTimeUnit.DAY, timeZone) else instant
        val exactSeconds = hours.toLong() * 3600 + minutes.toLong() * 60 + seconds.toLong()
        return withDays + (sign * exactSeconds).seconds
    }
}
