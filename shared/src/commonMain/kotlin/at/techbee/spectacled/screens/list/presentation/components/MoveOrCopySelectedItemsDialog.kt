package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.MoveOrCopyDialog
import at.techbee.spectacled.screens.core.presentation.components.MoveOrCopyMode
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.copy_selected
import spectacled.shared.generated.resources.copy_selected_info
import spectacled.shared.generated.resources.move_selected
import spectacled.shared.generated.resources.move_selected_warning

/** [MoveOrCopyDialog] for the entries selected in the list. */
@Composable
fun MoveOrCopySelectedItemsDialog(
    itemCount: Int,
    mode: MoveOrCopyMode,
    sourceCalendarId: Long,
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    MoveOrCopyDialog(
        mode = mode,
        title = stringResource(if (mode == MoveOrCopyMode.COPY) Res.string.copy_selected else Res.string.move_selected),
        message = pluralStringResource(
            if (mode == MoveOrCopyMode.COPY) Res.plurals.copy_selected_info else Res.plurals.move_selected_warning,
            itemCount,
            itemCount
        ),
        sourceCalendarId = sourceCalendarId,
        principals = principals,
        homeCollections = homeCollections,
        calendars = calendars,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Preview
@Composable
private fun MoveOrCopySelectedItemsDialog_Move_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopySelectedItemsDialog(
                itemCount = 3,
                mode = MoveOrCopyMode.MOVE,
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
private fun MoveOrCopySelectedItemsDialog_Copy_Single_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            MoveOrCopySelectedItemsDialog(
                itemCount = 1,
                mode = MoveOrCopyMode.COPY,
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
