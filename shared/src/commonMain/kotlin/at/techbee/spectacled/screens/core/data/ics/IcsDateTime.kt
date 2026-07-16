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
        fun now() = IcsDateTime(Clock.System.now(), false)

        fun today(): IcsDateTime {
            val todayInstant = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.atStartOfDayIn(TimeZone.UTC)
            return IcsDateTime(todayInstant, true)
        }

        fun todayAtStartOfDay(): IcsDateTime {
            val todayInstant = Clock.System.now().toLocalDateTime(TimeZone.UTC).date.atStartOfDayIn(TimeZone.UTC)
            return IcsDateTime(todayInstant, false)
        }

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
