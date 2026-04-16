package at.techbee.spectacled.screens.core.data.ics

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class IcsDateTime(
    val instant: Instant,
    val isDateOnly: Boolean,
    val timeZone: TimeZone? = null,
) {
    companion object {
        fun now() = IcsDateTime(Clock.System.now(), false)

    }

    fun toDatePickerMillis(deviceTimeZone: TimeZone): Long {

        val zone = if(isDateOnly) TimeZone.UTC else timeZone ?: deviceTimeZone

        val localDate = instant
            .toLocalDateTime(zone)
            .date

        // DatePicker expects midnight UTC of that calendar day
        return localDate
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    }

    fun updateWithDatePickerMillis(
        selectedMillis: Long,
        deviceTimeZone: TimeZone
    ): IcsDateTime {

        val selectedDate = Instant
            .fromEpochMilliseconds(selectedMillis)
            .toLocalDateTime(TimeZone.UTC)
            .date

        return this.copy(
            instant = if(isDateOnly) {
                selectedDate.atStartOfDayIn(TimeZone.UTC)
            } else {
                val originalLocal = instant.toLocalDateTime(timeZone ?: deviceTimeZone)  // selected timezone or time in timezone of the user

                val newLocal = LocalDateTime(
                    year = selectedDate.year,
                    month = selectedDate.month,
                    day = selectedDate.day,
                    hour = originalLocal.hour,
                    minute = originalLocal.minute,
                    second = originalLocal.second,
                    nanosecond = originalLocal.nanosecond
                )

                newLocal.toInstant(timeZone ?: deviceTimeZone)
            }
        )
    }

    /**
     * Determines the applicable [TimeZone] for this date-time instance based on its properties.
     *
     * The logic follows this priority:
     * 1. If [isDateOnly] is true, [TimeZone.UTC] is returned (standard for floating dates).
     * 2. If a specific [timeZone] is defined, that time zone is returned.
     * 3. Otherwise, the provided [deviceZone] is returned.
     *
     * @param deviceZone The fallback time zone to use if no specific zone is set and it is not a date-only value.
     * @return The [TimeZone] to be used for local time calculations.
     */
    fun effectiveZone(deviceZone: TimeZone = TimeZone.currentSystemDefault()): TimeZone =
        when {
            isDateOnly -> TimeZone.UTC
            timeZone != null -> timeZone
            else -> TimeZone.UTC
        }

    fun toLocalDateTime(): LocalDateTime = instant.toLocalDateTime(effectiveZone())

    /**
     * Returns a new [IcsDateTime] instance with the specified [newZone], preserving the local time
     * components (year, month, day, hour, etc.) by converting the current [instant] from its
     * effective time zone to the new one.
     *
     * @param newZone The new [TimeZone] to apply. If null, the time is treated as floating or
     * defaults to [deviceZone].
     * @param deviceZone The fallback [TimeZone] to use if the current or new zone is not explicitly defined.
     * @return A new [IcsDateTime] instance with the updated [instant] and [timeZone].
     */
    fun withZone(newZone: TimeZone?, deviceZone: TimeZone = TimeZone.currentSystemDefault()): IcsDateTime {
        val local = instant.toLocalDateTime(timeZone ?: TimeZone.UTC)

        return this.copy(
            instant = local.toInstant(newZone ?: TimeZone.UTC),
            timeZone = newZone
        )
    }
}