package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.presentation.components.DatePickerBottomSheet
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_date
import spectacled.shared.generated.resources.add_time
import spectacled.shared.generated.resources.date
import spectacled.shared.generated.resources.time
import spectacled.shared.generated.resources.timezone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeCard(
    icsDateTime: IcsDateTime?,
    enabled: Boolean,
    allowNoDate: Boolean,
    initializeWithDateOnly: Boolean,
    iconColor: Color?,
    suggestedTimezones: List<TimeZone>,
    onIcsDateTimeUpdated: (IcsDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    headerText: String? = null,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates
) {

    var showDatePickerBottomSheet by remember { mutableStateOf(false) }
    var showTimePickerBottomSheet by remember { mutableStateOf(false) }

    /*
    val buttonColors = ButtonDefaults.textButtonColors().copy(
        contentColor = iconColor?.let { getContentColorForColoredSurfaces(it) } ?: Color.Unspecified
    )
     */
    val buttonColors = ButtonDefaults.textButtonColors()

    if (showDatePickerBottomSheet) {
        DatePickerBottomSheet(
            icsDateTime = icsDateTime ?: if (initializeWithDateOnly) IcsDateTime.today() else IcsDateTime.todayAtStartOfDay(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            allowNoDate = allowNoDate,
            selectableDates = selectableDates,
            onDateSelected = {
                if (it == null || selectableDates.isSelectableDate(it.instant.toEpochMilliseconds()))
                    onIcsDateTimeUpdated(it)
            },
            onDismiss = { showDatePickerBottomSheet = false }
        )
    }

    if (showTimePickerBottomSheet && icsDateTime != null) {
        TimePickerBottomSheet(
            icsDateTime = icsDateTime,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            suggestedTimezones = suggestedTimezones,
            onTimeUpdated = { onIcsDateTimeUpdated(it) },
            onDismiss = { showTimePickerBottomSheet = false }
        )
    }

    ElevatedCard(
        modifier = modifier
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            TextButton(
                onClick = { showDatePickerBottomSheet = true },
                enabled = enabled,
                colors = buttonColors
            ) {
                Text(
                    text = headerText ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.rotate(270f)
                )
                Crossfade(icsDateTime != null) { datePresent ->
                    if (datePresent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = stringResource(Res.string.date)
                            )
                            icsDateTime?.let { Text(PlatformInstantFormatter(icsDateTime).formatLocalizedDate()) }
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.EditCalendar,
                            contentDescription = stringResource(Res.string.add_date)
                        )
                    }
                }
            }

            AnimatedVisibility(icsDateTime != null) {
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))
            }

            AnimatedVisibility(icsDateTime != null) {

                TextButton(
                    onClick = { showTimePickerBottomSheet = true },
                    enabled = enabled,
                    colors = buttonColors
                ) {

                    Crossfade(icsDateTime?.isDateOnly == false) { timePresent ->

                        if (timePresent) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = stringResource(Res.string.time)
                                )

                                Column {
                                    AnimatedVisibility(icsDateTime?.timeZone != null) {
                                        Text(
                                            text = icsDateTime?.timeZone?.id ?: "",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    icsDateTime?.let { Text(PlatformInstantFormatter(icsDateTime).formatLocalizedTime()) }
                                }
                            }

                        } else {
                            Icon(
                                imageVector = Icons.Outlined.MoreTime,
                                contentDescription = stringResource(Res.string.add_time)
                            )
                        }
                    }
                }
            }


            // show time in local timeZone
            AnimatedVisibility(icsDateTime?.timeZone != null) {
                VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 2.dp))
            }

            AnimatedVisibility(icsDateTime?.timeZone != null && icsDateTime.timeZone != TimeZone.currentSystemDefault()) {
                TextButton(
                    onClick = {  },
                    enabled = false // info only
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = stringResource(Res.string.timezone))

                        Column {
                            Text(
                                text = TimeZone.currentSystemDefault().id,
                                style = MaterialTheme.typography.labelSmall
                            )
                            icsDateTime?.let {
                                Text(
                                    text = PlatformInstantFormatter(icsDateTime.copy(timeZone = TimeZone.currentSystemDefault())).formatLocalizedDateTime()
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    /*
                AnimatedVisibility(icsDateTime?.isDateOnly == false && icsDateTime.timeZone != null) {
                    AssistChip(
                        onClick = { /* disabled, info only */ },
                        enabled = false,
                        leadingIcon = {
                            Icon(Icons.Outlined.Language, null)
                        },
                        label = {

                        },
                        colors = AssistChipDefaults.assistChipColors()
                            .copy(leadingIconContentColor = iconColor ?: MaterialTheme.colorScheme.primary)
                    )
                }
     */


}


@Preview
@Composable
private fun DateTimeCard_no_date_Preview() {
    DateTimeCard(
        icsDateTime = null,
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        initializeWithDateOnly = false,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {}
    )
}

@Preview
@Composable
private fun DateTimeCard_date_only_Preview() {
    DateTimeCard(
        icsDateTime = IcsDateTime.now().copy(isDateOnly = true),
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        initializeWithDateOnly = false,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {}
    )
}

@Preview
@Composable
private fun DDateTimeCard_date_time_Preview() {
    DateTimeCard(
        icsDateTime = IcsDateTime.now(),
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        initializeWithDateOnly = false,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {},
        headerText = "Due"
    )
}

@Preview
@Composable
private fun DateTimeCard_with_timezone_Preview() {
    DateTimeCard(
        icsDateTime = IcsDateTime.now().copy(timeZone = TimeZone.of(TimeZone.availableZoneIds.first())),
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        initializeWithDateOnly = false,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {}
    )
}

