package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.runtime.Composable
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.AlarmTriggerRelation
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.alarm_at_reference
import spectacled.shared.generated.resources.alarm_offset_after
import spectacled.shared.generated.resources.alarm_offset_before
import spectacled.shared.generated.resources.alarm_x_days
import spectacled.shared.generated.resources.alarm_x_hours
import spectacled.shared.generated.resources.alarm_x_minutes
import spectacled.shared.generated.resources.alarm_x_seconds
import spectacled.shared.generated.resources.alarm_x_weeks
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start

/**
 * The amount of this duration for display, e.g. "1 day 2 hours", without its sign - before/after
 * is said by the surrounding text (see [relativeTriggerText]). The components are shown as they
 * are stored, so "P1W" reads "1 week" and "P7D" reads "7 days".
 */
@Composable
fun IcsDuration.toDisplayString(): String = buildList {
    if (weeks != 0) add(pluralStringResource(Res.plurals.alarm_x_weeks, weeks, weeks))
    if (days != 0) add(pluralStringResource(Res.plurals.alarm_x_days, days, days))
    if (hours != 0) add(pluralStringResource(Res.plurals.alarm_x_hours, hours, hours))
    if (minutes != 0) add(pluralStringResource(Res.plurals.alarm_x_minutes, minutes, minutes))
    if (seconds != 0) add(pluralStringResource(Res.plurals.alarm_x_seconds, seconds, seconds))
}.joinToString(" ")

/**
 * A relative trigger in words: "At Due", "15 minutes before Due", "1 day after Start". Used by the
 * alarm card and the add sheet, so both always describe an alarm the same way.
 */
@Composable
fun relativeTriggerText(trigger: AlarmTrigger.Relative): String {
    val reference = stringResource(
        if (trigger.relatedTo == AlarmTriggerRelation.END) Res.string.date_due else Res.string.date_start
    )
    return if (trigger.duration.isZero())
        stringResource(Res.string.alarm_at_reference, reference)
    else
        stringResource(
            if (trigger.duration.isNegative) Res.string.alarm_offset_before else Res.string.alarm_offset_after,
            trigger.duration.toDisplayString(),
            reference
        )
}
