package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.CalendarSelector
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.move
import spectacled.shared.generated.resources.move_selected
import spectacled.shared.generated.resources.move_selected_warning

@Composable
fun MoveSelectedItemsDialog(
    itemCount: Int,
    sourceCalendarId: Long,
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {

    var selectedCalendarId by remember { mutableStateOf(sourceCalendarId) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCalendarId) },
                enabled = sourceCalendarId != selectedCalendarId
            ) {
                Text(stringResource(Res.string.move))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        icon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, null) },
        title = {
            Text(stringResource(Res.string.move_selected))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {

                CalendarSelector(
                    principals = principals,
                    homeCollections = homeCollections,
                    calendars = calendars,
                    selectedCalendarId = selectedCalendarId,
                    onCalendarIdSelected = { selectedCalendarId = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(pluralStringResource(Res.plurals.move_selected_warning, itemCount, itemCount))
            }
        }
    )
}

@Preview
@Composable
private fun MoveSelectedItemsDialog_Multiple_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveSelectedItemsDialog(
                itemCount = 3,
                sourceCalendarId = 0L,
                calendars = listOf(Calendar.getCalendarForPreview()),
                homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
                principals = listOf(Principal.getPrincipalForPreview()),
                onConfirm = { },
                onDismiss = {}
            )
        }
    }
}


@Preview
@Composable
private fun MoveSelectedItemsDialog_Single_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveSelectedItemsDialog(
                itemCount = 1,
                sourceCalendarId = 0L,
                calendars = listOf(Calendar.getCalendarForPreview()),
                homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
                principals = listOf(Principal.getPrincipalForPreview()),
                onConfirm = { },
                onDismiss = {}
            )
        }
    }
}
