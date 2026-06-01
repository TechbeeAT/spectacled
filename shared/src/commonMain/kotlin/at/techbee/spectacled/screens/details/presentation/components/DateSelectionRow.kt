package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionRow(
    icsDateTime: IcsDateTime?,
    enabled: Boolean,
    allowNoDate: Boolean,
    iconColor: Color?,
    suggestedTimezones: List<TimeZone>,
    onIcsDateTimeUpdated: (IcsDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    headerText: String? = null
) {

    var showDatePickerBottomSheet by remember { mutableStateOf(false) }
    var showTimePickerBottomSheet by remember { mutableStateOf(false) }


    if (showDatePickerBottomSheet) {
        DatePickerBottomSheet(
            icsDateTime = icsDateTime ?: IcsDateTime.today(),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            allowNoDate = allowNoDate,
            onDateSelected = { onIcsDateTimeUpdated(it) },
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

    Column(
        modifier = modifier
    ) {

        headerText?.let {
            Text(
                text = headerText,
                style = MaterialTheme.typography.labelSmall
            )
        }


        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            AssistChip(
                onClick = { showDatePickerBottomSheet = true },
                leadingIcon = {
                    if (icsDateTime != null)
                        Icon(Icons.Outlined.CalendarToday, null)
                },
                label = {
                    Crossfade(icsDateTime != null) { datePresent ->
                        if (datePresent) {
                            icsDateTime?.let { Text(PlatformInstantFormatter(icsDateTime).formatLocalizedDate()) }
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.EditCalendar,
                                contentDescription = stringResource(Res.string.add_date),
                                tint = iconColor ?: MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                enabled = enabled,
                colors = AssistChipDefaults.assistChipColors()
                    .copy(leadingIconContentColor = iconColor ?: MaterialTheme.colorScheme.primary)
            )

            AnimatedVisibility(icsDateTime != null) {
                AssistChip(
                    onClick = { showTimePickerBottomSheet = true },
                    leadingIcon = {
                        if (icsDateTime?.isDateOnly == false)
                            Icon(Icons.Outlined.Schedule, null)
                    },
                    enabled = enabled,
                    label = {
                        Crossfade(icsDateTime?.isDateOnly == true) { dateOnly ->
                            if (dateOnly) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreTime,
                                    contentDescription = stringResource(Res.string.add_time),
                                    tint = iconColor ?: MaterialTheme.colorScheme.primary
                                )
                            } else {
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
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = iconColor ?: MaterialTheme.colorScheme.primary)
                )
            }

            AnimatedVisibility(icsDateTime?.isDateOnly == false && icsDateTime.timeZone != null) {
                AssistChip(
                    onClick = { /* disabled, info only */ },
                    enabled = false,
                    leadingIcon = {
                        Icon(Icons.Outlined.Language, null)
                    },
                    label = {
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
                    },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = iconColor ?: MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}





@Preview
@Composable
private fun DateSelectionRow_no_date_Preview() {
    DateSelectionRow(
        icsDateTime = null,
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {}
    )
}

@Preview
@Composable
private fun DateSelectionRow_date_only_Preview() {
    DateSelectionRow(
        icsDateTime = IcsDateTime.now().copy(isDateOnly = true),
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {}
    )
}

@Preview
@Composable
private fun DateSelectionRow_date_time_Preview() {
    DateSelectionRow(
        icsDateTime = IcsDateTime.now(),
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {},
        headerText = "Due"
    )
}

@Preview
@Composable
private fun DateSelectionRow_with_timezone_Preview() {
    DateSelectionRow(
        icsDateTime = IcsDateTime.now().copy(timeZone = TimeZone.currentSystemDefault()),
        enabled = true,
        iconColor = null,
        allowNoDate = true,
        suggestedTimezones = emptyList(),
        onIcsDateTimeUpdated = {}
    )
}

