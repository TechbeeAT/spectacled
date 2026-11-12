package at.techbee.spectacled.screens.core.data.ics

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Clock
import kotlin.time.Instant

@Serializable
data class IcsDateTime(
    val instant: Instant,
    val isDateOnly: Boolean,
    @Serializable(with = TimeZoneSerializer::class)
    val timeZone: TimeZone? = null,
) {
    companion object {
        /**
         * The current moment in UTC (serialized with a trailing `Z`). RFC 5545 requires UTC for
         * DTSTAMP, CREATED and LAST-MODIFIED, so use this for those and other technical timestamps.
         */
        fun nowUtc() = IcsDateTime(Clock.System.now(), false, TimeZone.UTC)

        /**
         * The current moment bound to [timeZone] (serialized with `TZID=`), e.g. the DTSTART of a
         * new journal in the timezone of the device.
         */
        fun nowInZone(timeZone: TimeZone = TimeZone.currentSystemDefault()) =
            IcsDateTime(Clock.System.now(), false, timeZone)

        /**
         * The current wall-clock time in [deviceTimeZone] as a floating DATE-TIME (no timezone).
         * Floating values store their wall-clock time as UTC, so the local time is re-anchored
         * to UTC here instead of using the raw instant.
         */
        fun nowFloating(
            deviceTimeZone: TimeZone = TimeZone.currentSystemDefault(),
            now: Instant = Clock.System.now()
        ) = IcsDateTime(
            now.toLocalDateTime(deviceTimeZone).toInstant(TimeZone.UTC),
            false,
            null
        )

        /** Today's date in [deviceTimeZone] as a DATE value. */
        fun today(
            deviceTimeZone: TimeZone = TimeZone.currentSystemDefault(),
            now: Instant = Clock.System.now()
        ): IcsDateTime {
            val todayInstant = now.toLocalDateTime(deviceTimeZone).date.atStartOfDayIn(TimeZone.UTC)
            return IcsDateTime(todayInstant, true)
        }

        /** Midnight of today's date in [deviceTimeZone] as a floating DATE-TIME. */
        fun todayAtStartOfDay(
            deviceTimeZone: TimeZone = TimeZone.currentSystemDefault(),
            now: Instant = Clock.System.now()
        ): IcsDateTime = today(deviceTimeZone, now).copy(isDateOnly = false)
    }

    /**
     * Midnight UTC of the calendar day of this value, as expected by the Material DatePicker.
     * The day is read in [effectiveZone], i.e. a floating value keeps its wall-clock day.
     */
    fun toDatePickerMillis(): Long =
        instant
            .toLocalDateTime(effectiveZone())
            .date
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()

    /**
     * Moves this value to the day selected in the DatePicker ([selectedMillis] is midnight UTC
     * of that day), keeping the time of day and the timezone (or floating-ness) unchanged.
     */
    fun updateWithDatePickerMillis(selectedMillis: Long): IcsDateTime {

        val selectedDate = Instant
            .fromEpochMilliseconds(selectedMillis)
            .toLocalDateTime(TimeZone.UTC)
            .date

        val originalLocal = toLocalDateTime()
        val newLocal = LocalDateTime(
            year = selectedDate.year,
            month = selectedDate.month,
            day = selectedDate.day,
            hour = originalLocal.hour,
            minute = originalLocal.minute,
            second = originalLocal.second,
            nanosecond = originalLocal.nanosecond
        )

        return this.copy(instant = newLocal.toInstant(effectiveZone()))
    }

    /**
     * Determines the applicable [TimeZone] for this date-time instance based on its properties.
     *
     * The logic follows this priority:
     * 1. If [isDateOnly] is true, [TimeZone.UTC] is returned (standard for floating dates).
     * 2. If a specific [timeZone] is defined, that time zone is returned.
     * 3. Otherwise [TimeZone.UTC] is returned.
     *
     * @return The [TimeZone] to be used for local time calculations.
     */
    fun effectiveZone(): TimeZone =
        when {
            isDateOnly -> TimeZone.UTC
            timeZone != null -> timeZone
            else -> TimeZone.UTC
        }

    fun toLocalDateTime(): LocalDateTime = instant.toLocalDateTime(effectiveZone())

    /**
     * The actual point in time of this value. DATE and floating DATE-TIME values have no zone of
     * their own (they are stored as UTC wall-clock), they are resolved in [deviceTimeZone].
     */
    fun resolvedInstant(deviceTimeZone: TimeZone): Instant =
        if (isDateOnly || timeZone == null)
            instant.toLocalDateTime(TimeZone.UTC).toInstant(deviceTimeZone)
        else
            instant

    /**
     * This value as a point in time shown in [deviceTimeZone], for technical timestamps
     * (CREATED, LAST-MODIFIED, sync time) that are stored in UTC. A floating timestamp is taken as
     * UTC, like the parser does. Not meant for DATE values.
     */
    fun inDeviceZone(deviceTimeZone: TimeZone = TimeZone.currentSystemDefault()): IcsDateTime =
        IcsDateTime(instant, false, deviceTimeZone)

    /**
     * Returns a new [IcsDateTime] instance with the specified [newZone], preserving the local time
     * components (year, month, day, hour, etc.) by converting the current [instant] from its
     * effective time zone to the new one.
     *
     * @param newZone The new [TimeZone] to apply. If null, the time is treated as floating or
     * defaults to [TimeZone.UTC].
     * @return A new [IcsDateTime] instance with the updated [instant] and [timeZone].
     */
    fun withZone(newZone: TimeZone?): IcsDateTime {
        val local = instant.toLocalDateTime(timeZone ?: TimeZone.UTC)

        return this.copy(
            instant = local.toInstant(newZone ?: TimeZone.UTC),
            timeZone = newZone
        )
    }
    
    fun asDateOnly(): IcsDateTime {
        val localDate = toLocalDateTime().date
        return IcsDateTime(
            instant = localDate.atStartOfDayIn(TimeZone.UTC),
            isDateOnly = true,
            timeZone = null
        )
    }

    fun asDateTime(): IcsDateTime {
        val localDate = toLocalDateTime().date
        return this.copy(
            instant = localDate.atStartOfDayIn(effectiveZone()),
            isDateOnly = false
        )
    }
}

object TimeZoneSerializer : KSerializer<TimeZone> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TimeZone", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: TimeZone) = encoder.encodeString(value.id)

    // Fall back to UTC for unknown or unresolvable zone ids instead of throwing, mirroring the ICS
    // parser (IcalEntryIcsParser). On web the tz database is loaded at startup (SecureStorageReadyGate);
    // this guard keeps a corrupt or unexpected id from crashing while loading a stored entry.
    override fun deserialize(decoder: Decoder): TimeZone {
        val id = decoder.decodeString()
        return runCatching { TimeZone.of(id) }.getOrDefault(TimeZone.UTC)
    }
}
