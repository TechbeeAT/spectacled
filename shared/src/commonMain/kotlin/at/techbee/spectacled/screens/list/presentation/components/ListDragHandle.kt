package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.drag_handle

@Composable
fun ListDragHandle(scope: ReorderableCollectionItemScope) {

    /*
    VerticalDragHandle(
        modifier = with(scope) {
            Modifier.draggableHandle()
        }
    )
     */

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
