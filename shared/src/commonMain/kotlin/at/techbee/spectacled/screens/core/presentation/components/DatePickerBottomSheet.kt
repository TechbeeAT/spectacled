package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_date
import kotlin.time.Clock.System

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerBottomSheet(
    icsDateTime: IcsDateTime,
    sheetState: SheetState,
    allowNoDate: Boolean,
    onDateSelected: (IcsDateTime?) -> Unit,
    onDismiss: () -> Unit,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates
) {

    val deviceTimeZone = TimeZone.currentSystemDefault()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = icsDateTime.toDatePickerMillis(deviceTimeZone),
        selectableDates = selectableDates
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
        menuAction = {
            if(allowNoDate) {
                TextButton(
                    onClick = { onDateSelected(null) }
                ) {
                    Text(stringResource(Res.string.no_date))
                }
            }

        }
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
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        allowNoDate = false,
        onDateSelected = {},
        onDismiss = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun DatePickerBottomSheet_allow_no_date_Preview() {

    DatePickerBottomSheet(
        icsDateTime = IcsDateTime(System.now(), false),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        allowNoDate = true,
        onDateSelected = {},
        onDismiss = {}
    )
}