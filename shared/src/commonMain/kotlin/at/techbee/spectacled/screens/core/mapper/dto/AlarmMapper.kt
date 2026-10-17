package at.techbee.spectacled.screens.core.mapper.dto

import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.mapper.ics.parseAlarm
import at.techbee.spectacled.screens.core.mapper.ics.serializeAlarm
import at.techbee.spectacled.screens.core.mapper.ics.splitNestedComponents
import at.techbee.spectacled.sqldelight.AlarmDto
import kotlin.time.Instant

private const val ICS_BLOCK_LINE_SEPARATOR = "\n"

/** The stored VALARM block is parsed with the same code as on sync, so both can't drift apart. */
fun AlarmDto.toDomain(): Alarm =
    parseAlarm(splitNestedComponents(icsBlock.split(ICS_BLOCK_LINE_SEPARATOR)).alarmBlocks.firstOrNull() ?: emptyList())

/**
 * [triggerAt] is the alarm's fire time for its entry (see [Alarm.triggerTime]), [lastNotifiedAt]
 * the local-only notification state carried over from the previous row, if any.
 */
fun Alarm.toDto(icalEntryId: Long, triggerAt: Instant?, lastNotifiedAt: Long? = null): AlarmDto =
    AlarmDto(
        id = 0L,
        icalEntryId = icalEntryId,
        uid = uid,
        action = action,
        icsBlock = toIcsBlock(),
        triggerAt = triggerAt?.toEpochMilliseconds(),
        lastNotifiedAt = lastNotifiedAt
    )

fun Alarm.toIcsBlock(): String = serializeAlarm(this).joinToString(ICS_BLOCK_LINE_SEPARATOR)
