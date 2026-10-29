package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.MoveOrCopyDialog
import at.techbee.spectacled.screens.core.presentation.components.MoveOrCopyMode
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.copy_entry_info
import spectacled.shared.generated.resources.copy_entry_x
import spectacled.shared.generated.resources.move_entry_warning
import spectacled.shared.generated.resources.move_entry_x

/** [MoveOrCopyDialog] for a single entry on the details screen. */
@Composable
fun MoveOrCopyIcalEntryDialog(
    icalEntry: IcalEntry,
    mode: MoveOrCopyMode,
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    MoveOrCopyDialog(
        mode = mode,
        title = stringResource(
            if (mode == MoveOrCopyMode.COPY) Res.string.copy_entry_x else Res.string.move_entry_x,
            icalEntry.summary ?: icalEntry.description ?: ""
        ),
        message = stringResource(if (mode == MoveOrCopyMode.COPY) Res.string.copy_entry_info else Res.string.move_entry_warning),
        sourceCalendarId = icalEntry.calendarId,
        principals = principals,
        homeCollections = homeCollections,
        calendars = calendars,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Preview
@Composable
private fun MoveOrCopyIcalEntryDialog_Move_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopyIcalEntryDialog(
                icalEntry = IcalEntry.getSampleIcalEntry(),
                mode = MoveOrCopyMode.MOVE,
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
private fun MoveOrCopyIcalEntryDialog_Copy_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopyIcalEntryDialog(
                icalEntry = IcalEntry.getSampleIcalEntry(),
                mode = MoveOrCopyMode.COPY,
                calendars = listOf(Calendar.getCalendarForPreview()),
                homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
                principals = listOf(Principal.getPrincipalForPreview()),
                onConfirm = { },
                onDismiss = {}
            )
        }
    }
}
