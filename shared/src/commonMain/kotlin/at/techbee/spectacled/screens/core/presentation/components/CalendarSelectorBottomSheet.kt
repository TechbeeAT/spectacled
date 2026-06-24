package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSelectorBottomSheet(
    sheetState: SheetState,
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    selectedCalendarId: Long?,
    onCalendarIdSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {

    BottomSheetWithMenu(
        sheetState = sheetState,
        allowClose = calendars.any { it.id == selectedCalendarId },
        onDismiss = { onDismiss() },
        menuAction = {
            /*
            if(allowNoDate) {
                TextButton(
                    onClick = { onDateSelected(null) }
                ) {
                    Text(stringResource(Res.string.done))
                }
            }

             */

        }
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {

            Text(
                text = "Select calendar/folder",
                style = MaterialTheme.typography.headlineSmall
            )

            CalendarSelector(
                principals = principals,
                homeCollections = homeCollections,
                calendars = calendars,
                selectedCalendarId = selectedCalendarId,
                onCalendarIdSelected = { onCalendarIdSelected(it) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CalendarSelectorBottomSheet_Preview() {

    CalendarSelectorBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        calendars = listOf(Calendar.getCalendarForPreview()),
        homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
        principals = listOf(Principal.getPrincipalForPreview()),
        selectedCalendarId = null,
        onCalendarIdSelected = {},
        onDismiss = {}
    )
}
