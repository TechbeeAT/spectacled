package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_time
import spectacled.shared.generated.resources.no_timezone
import spectacled.shared.generated.resources.timezone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerBottomSheet(
    icsDateTime: IcsDateTime,
    sheetState: SheetState,
    suggestedTimezones: List<TimeZone> = emptyList(),
    onTimeUpdated: (IcsDateTime) -> Unit,
    onDismiss: () -> Unit
) {

    val localDateTime = icsDateTime.toLocalDateTime()

    val timePickerState = rememberTimePickerState(
        localDateTime.hour,
        localDateTime.minute
    )
    var showTimezoneDropdown by mutableStateOf(false)

    LaunchedEffect(timePickerState.minute, timePickerState.hour) {
        val newLocalDateTime = LocalDateTime(
            year = localDateTime.year,
            month = localDateTime.month,
            day = localDateTime.day,
            hour = timePickerState.hour,
            minute = timePickerState.minute
        )
        val newInstant = newLocalDateTime.toInstant(icsDateTime.timeZone ?: TimeZone.UTC)
        onTimeUpdated(icsDateTime.copy(instant = newInstant, isDateOnly = false))
    }

    BottomSheetWithMenu(
        sheetState = sheetState,
        onDismiss = { onDismiss() },
        menuAction = { TextButton(onClick = {
            val newInstant = localDateTime.date.atStartOfDayIn(TimeZone.UTC)

            onTimeUpdated(
                icsDateTime.copy(
                    instant = newInstant,
                    isDateOnly = true,
                    timeZone = null
                )
            )
            onDismiss()
        }) { Text(stringResource(Res.string.no_time)) } }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            TimePicker(timePickerState)

            AssistChip(
                onClick = { showTimezoneDropdown = true },
                label = { Text(icsDateTime.timeZone?.id?: stringResource(Res.string.no_timezone)) },
                leadingIcon = { Icon(Icons.Outlined.Language, stringResource(Res.string.timezone)) },
                trailingIcon = {
                    DropdownMenu(
                        expanded =  showTimezoneDropdown,
                        onDismissRequest = { showTimezoneDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.no_timezone)) },
                            onClick = {
                                onTimeUpdated(icsDateTime.withZone(null))
                                showTimezoneDropdown = false
                            },
                            trailingIcon = {
                                if(icsDateTime.timeZone == null)
                                    Icon(Icons.Outlined.Check, null)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))

                        if(suggestedTimezones.isNotEmpty()) {
                            suggestedTimezones.forEach { timezone ->
                                DropdownMenuItem(
                                    text = { Text(timezone.id) },
                                    onClick = {
                                        onTimeUpdated(icsDateTime.withZone(timezone))
                                        showTimezoneDropdown = false
                                    },
                                    trailingIcon = {
                                        if(icsDateTime.timeZone == timezone)
                                            Icon(Icons.Outlined.Check, null)
                                    }
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp))
                        }

                        TimeZone.availableZoneIds.toList().sorted().forEach { timezone ->
                            DropdownMenuItem(
                                text = { Text(timezone) },
                                onClick = {
                                    onTimeUpdated(icsDateTime.withZone(TimeZone.of(timezone)))
                                    showTimezoneDropdown = false
                                },
                                trailingIcon = {
                                    if(icsDateTime.timeZone?.id == timezone)
                                        Icon(Icons.Outlined.Check, null)
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun TimePickerBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            TimePickerBottomSheet(
                icsDateTime = IcsDateTime.now(),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                onTimeUpdated = {},
                onDismiss = {}
            )
        }
    }
}


