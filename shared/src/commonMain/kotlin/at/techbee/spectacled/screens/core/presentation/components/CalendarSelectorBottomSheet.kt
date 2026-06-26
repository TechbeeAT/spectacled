package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.theme.AppTheme

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
        headline = "Select calendar/folder",
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun CalendarSelectorBottomSheet_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
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
    }

}
