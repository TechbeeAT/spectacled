package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.AlarmTriggerRelation
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add
import spectacled.shared.generated.resources.add_alarm
import spectacled.shared.generated.resources.alarm
import spectacled.shared.generated.resources.alarm_absolute
import spectacled.shared.generated.resources.alarm_after
import spectacled.shared.generated.resources.alarm_before
import spectacled.shared.generated.resources.alarm_fires_at
import spectacled.shared.generated.resources.alarm_fires_in_past
import spectacled.shared.generated.resources.alarm_needs_reference_date
import spectacled.shared.generated.resources.alarm_presets
import spectacled.shared.generated.resources.alarm_relative
import spectacled.shared.generated.resources.alarm_unit_days
import spectacled.shared.generated.resources.alarm_unit_hours
import spectacled.shared.generated.resources.alarm_unit_minutes
import spectacled.shared.generated.resources.alarm_unit_weeks
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start
import kotlin.time.Clock
import kotlin.time.Instant

private enum class AlarmMode { RELATIVE, ABSOLUTE }

/** The units offered for a relative alarm, mapped onto the RFC 5545 duration components. */
private enum class AlarmOffsetUnit(val nameRes: StringResource) {
    MINUTES(Res.string.alarm_unit_minutes),
    HOURS(Res.string.alarm_unit_hours),
    DAYS(Res.string.alarm_unit_days),
    WEEKS(Res.string.alarm_unit_weeks);

    fun toDuration(amount: Int, isBefore: Boolean) = when (this) {
        MINUTES -> IcsDuration(isNegative = isBefore, minutes = amount)
        HOURS -> IcsDuration(isNegative = isBefore, hours = amount)
        DAYS -> IcsDuration(isNegative = isBefore, days = amount)
        WEEKS -> IcsDuration(isNegative = isBefore, weeks = amount)
    }
}

/** Quick picks shown above the custom offset, as (amount, unit). All of them are "before". */
private val PRESETS = listOf(
    0 to AlarmOffsetUnit.MINUTES,
    15 to AlarmOffsetUnit.MINUTES,
    1 to AlarmOffsetUnit.HOURS,
    1 to AlarmOffsetUnit.DAYS
)

/**
 * Lets the user add a DISPLAY alarm to [icalEntry], either
 *  - relative to its start or due date ("15 minutes before due"), which keeps following that date
 *    when it is changed later, or
 *  - at an absolute date and time (stored in UTC, as RFC 5545 requires for absolute triggers).
 *
 * The sheet only builds the [Alarm]; the preview line uses [Alarm.triggerTime], the same
 * computation that decides when the alarm fires, so what the user sees is what will happen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmBottomSheet(
    icalEntry: IcalEntry,
    onAlarmConfirmed: (Alarm) -> Unit,
    onDismiss: () -> Unit
) {
    val deviceTimeZone = remember { TimeZone.currentSystemDefault() }

    // RELATED=END means DUE only for tasks; Alarm.triggerTime() treats anything else as undefined.
    val startAvailable = icalEntry.dtStart != null
    val dueAvailable = icalEntry.isTask() && icalEntry.due != null

    var mode by rememberSaveable {
        mutableStateOf(if (startAvailable || dueAvailable) AlarmMode.RELATIVE else AlarmMode.ABSOLUTE)
    }

    // Relative
    var amountText by rememberSaveable { mutableStateOf("15") }
    var unit by rememberSaveable { mutableStateOf(AlarmOffsetUnit.MINUTES) }
    var isBefore by rememberSaveable { mutableStateOf(true) }
    var relatedTo by rememberSaveable {
        mutableStateOf(if (dueAvailable) AlarmTriggerRelation.END else AlarmTriggerRelation.START)
    }

    // Absolute - starts at the next full hour, in the device time zone.
    var absoluteDateTime by remember {
        mutableStateOf(IcsDateTime(nextFullHourInstant(deviceTimeZone), isDateOnly = false, timeZone = deviceTimeZone))
    }

    val description = icalEntry.summary?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.alarm)
    val amount = amountText.toIntOrNull()?.takeIf { it >= 0 }

    val alarm: Alarm? = when (mode) {
        AlarmMode.RELATIVE -> amount?.let {
            Alarm(
                trigger = AlarmTrigger.Relative(unit.toDuration(it, isBefore), relatedTo),
                description = description
            )
        }
        AlarmMode.ABSOLUTE -> Alarm(
            trigger = AlarmTrigger.Absolute(absoluteDateTime.resolvedInstant(deviceTimeZone)),
            description = description
        )
    }
    val triggerTime = alarm?.triggerTime(icalEntry, deviceTimeZone)

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        headline = stringResource(Res.string.add_alarm),
        menuActionRight = {
            TextButton(
                onClick = {
                    alarm?.let { onAlarmConfirmed(it) }
                    onDismiss()
                },
                enabled = alarm != null && triggerTime != null && triggerTime > Clock.System.now()
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        menuActionLeft = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AlarmMode.entries.forEachIndexed { index, alarmMode ->
                    SegmentedButton(
                        selected = mode == alarmMode,
                        onClick = { mode = alarmMode },
                        shape = SegmentedButtonDefaults.itemShape(index, AlarmMode.entries.size),
                        label = {
                            Text(
                                stringResource(
                                    when (alarmMode) {
                                        AlarmMode.RELATIVE -> Res.string.alarm_relative
                                        AlarmMode.ABSOLUTE -> Res.string.alarm_absolute
                                    }
                                )
                            )
                        }
                    )
                }
            }

            when (mode) {
                AlarmMode.RELATIVE -> {
                    if (!startAvailable && !dueAvailable) {
                        Text(
                            text = stringResource(Res.string.alarm_needs_reference_date),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        RelativeAlarmEditor(
                            amountText = amountText,
                            unit = unit,
                            isBefore = isBefore,
                            relatedTo = relatedTo,
                            startAvailable = startAvailable,
                            dueAvailable = dueAvailable,
                            onAmountTextChanged = { amountText = it.filter(Char::isDigit).take(4) },
                            onUnitChanged = { unit = it },
                            onIsBeforeChanged = { isBefore = it },
                            onRelatedToChanged = { relatedTo = it },
                            onPresetSelected = { (presetAmount, presetUnit) ->
                                amountText = presetAmount.toString()
                                unit = presetUnit
                                isBefore = true
                            }
                        )
                    }
                }
                AlarmMode.ABSOLUTE -> {
                    DateTimeCard(
                        icsDateTime = absoluteDateTime,
                        enabled = true,
                        allowNoDate = false,
                        initializeWithDateOnly = false,
                        suggestedTimezones = emptyList(),
                        onIcsDateTimeUpdated = { updated -> updated?.let { absoluteDateTime = it } },
                    )
                }
            }

            HorizontalDivider()

            AlarmPreview(triggerTime = triggerTime, deviceTimeZone = deviceTimeZone)
        }
    }
}

@Composable
private fun RelativeAlarmEditor(
    amountText: String,
    unit: AlarmOffsetUnit,
    isBefore: Boolean,
    relatedTo: AlarmTriggerRelation,
    startAvailable: Boolean,
    dueAvailable: Boolean,
    onAmountTextChanged: (String) -> Unit,
    onUnitChanged: (AlarmOffsetUnit) -> Unit,
    onIsBeforeChanged: (Boolean) -> Unit,
    onRelatedToChanged: (AlarmTriggerRelation) -> Unit,
    onPresetSelected: (Pair<Int, AlarmOffsetUnit>) -> Unit
) {
    Text(
        text = stringResource(Res.string.alarm_presets),
        style = MaterialTheme.typography.labelLarge
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Quick picks: "At Due", "15 minutes before Due", ...
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            PRESETS.forEach { preset ->
                val (presetAmount, presetUnit) = preset
                FilterChip(
                    onClick = { onPresetSelected(preset) },
                    selected = amountText == presetAmount.toString() && unit == presetUnit && isBefore,
                    label = {
                        Text(relativeTriggerText(AlarmTrigger.Relative(presetUnit.toDuration(presetAmount, isBefore = true), relatedTo)))
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Custom: [ 15 ] Minutes | Hours | Days | Weeks
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountTextChanged,
                isError = amountText.toIntOrNull() == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(88.dp)
            )
            SingleChoiceSegmentedButtonRow {
                AlarmOffsetUnit.entries.forEachIndexed { index, offsetUnit ->
                    SegmentedButton(
                        selected = unit == offsetUnit,
                        onClick = { onUnitChanged(offsetUnit) },
                        shape = SegmentedButtonDefaults.itemShape(index, AlarmOffsetUnit.entries.size),
                        label = {
                            Text(
                                text = stringResource(offsetUnit.nameRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    /*
                    FilterChip(
                        selected = unit == offsetUnit,
                        onClick = { onUnitChanged(offsetUnit) },
                        label = { Text(stringResource(offsetUnit.nameRes)) }
                    )
                     */
                }
            }
            /*
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AlarmOffsetUnit.entries.forEach { offsetUnit ->
                    FilterChip(
                        selected = unit == offsetUnit,
                        onClick = { onUnitChanged(offsetUnit) },
                        label = { Text(stringResource(offsetUnit.nameRes)) }
                    )
                }
            }

             */
        }

        // before | after    Start | Due
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = isBefore,
                    onClick = { onIsBeforeChanged(true) },
                    label = { Text(stringResource(Res.string.alarm_before)) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                )
                SegmentedButton(
                    selected = !isBefore,
                    onClick = { onIsBeforeChanged(false) },
                    label = { Text(stringResource(Res.string.alarm_after)) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                )
            }

            //Text("·", modifier = Modifier.padding(horizontal = 4.dp))

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = relatedTo == AlarmTriggerRelation.START,
                    enabled = startAvailable,
                    onClick = { onRelatedToChanged(AlarmTriggerRelation.START) },
                    label = { Text(stringResource(Res.string.date_start)) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                )
                SegmentedButton(
                    selected = relatedTo == AlarmTriggerRelation.END,
                    enabled = dueAvailable,
                    onClick = { onRelatedToChanged(AlarmTriggerRelation.END) },
                    label = { Text(stringResource(Res.string.date_due)) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                )
            }
        }

        // Summary of the custom selection, e.g. "15 minutes before Due"
        amountText.toIntOrNull()?.let { amount ->
            Text(
                text = relativeTriggerText(AlarmTrigger.Relative(unit.toDuration(amount, isBefore), relatedTo)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** "Alarm on Mon, 5 Oct 2026, 08:45", or why the alarm wouldn't go off. */
@Composable
private fun AlarmPreview(triggerTime: Instant?, deviceTimeZone: TimeZone) {
    when {
        triggerTime == null -> Unit     // nothing to fire relative to - the editor already says so
        triggerTime < Clock.System.now() -> Text(
            text = stringResource(Res.string.alarm_fires_in_past),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        else -> Text(
            text = stringResource(
                Res.string.alarm_fires_at,
                IcsDateTime(triggerTime, isDateOnly = false, timeZone = deviceTimeZone).formatLocalized(IcsDateTimeFormat.DATE_TIME)
            ),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun nextFullHourInstant(timeZone: TimeZone): Instant {
    val now = Clock.System.now().toLocalDateTime(timeZone)
    val startOfHour = LocalDateTime(now.date, LocalTime(now.hour, 0))
    return startOfHour.toInstant(timeZone).plus(1, DateTimeUnit.HOUR, timeZone)
}


@Preview
@Composable
private fun AddAlarmBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            AddAlarmBottomSheet(
                icalEntry = IcalEntry.getSampleTask(),
                onAlarmConfirmed = { },
                onDismiss = { }
            )
        }
    }
}

@Preview
@Composable
private fun AddAlarmBottomSheet_NoDates_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            AddAlarmBottomSheet(
                icalEntry = IcalEntry.newTask(),
                onAlarmConfirmed = { },
                onDismiss = { }
            )
        }
    }
}
