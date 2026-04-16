package at.techbee.spectacled.screens.note.presentation.notelist.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.drag_handle
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope

@Composable
fun NoteDragHandle(scope: ReorderableCollectionItemScope) {
    IconButton(
        onClick = {},
        modifier = with(scope) {
            Modifier.draggableHandle()
        }
    ) {
        Icon(
            imageVector = Icons.Outlined.DragIndicator,
            contentDescription = stringResource(Res.string.drag_handle)
        )
    }
}
