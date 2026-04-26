package at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import kotlinx.datetime.TimeZone
import kotlin.time.Clock.System

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerBottomSheet(
    icsDateTime: IcsDateTime,
    sheetState: SheetState,
    onDateSelected: (IcsDateTime) -> Unit,
    onDismiss: () -> Unit
) {

    val deviceTimeZone = TimeZone.currentSystemDefault()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = icsDateTime.toDatePickerMillis(deviceTimeZone)
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { millis ->
            val updated = icsDateTime.updateWithDatePickerMillis(millis, deviceTimeZone)
            onDateSelected(updated)
        }
    }

    BottomSheetWithMenu(
        sheetState = sheetState,
        onDismiss = { onDismiss() },
    ) {
        DatePicker(datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun DatePickerBottomSheet_Preview() {
    DatePickerBottomSheet(
        icsDateTime = IcsDateTime(System.now(), false),
        sheetState = rememberModalBottomSheetState(),
        onDateSelected = {},
        onDismiss = {}
    )
}
