package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_time

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerBottomSheet(
    icsDateTime: IcsDateTime,
    sheetState: SheetState,
    onTimeUpdated: (IcsDateTime) -> Unit,
    onDismiss: () -> Unit
) {

    val localDateTime = icsDateTime.toLocalDateTime()

    val timePickerState = rememberTimePickerState(
        localDateTime.hour,
        localDateTime.minute
    )

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
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun TimePickerBottomSheet_Preview() {
    TimePickerBottomSheet(
        icsDateTime = IcsDateTime.now(),
        sheetState = rememberModalBottomSheetState(),
        onTimeUpdated = {},
        onDismiss = {}
    )
}


