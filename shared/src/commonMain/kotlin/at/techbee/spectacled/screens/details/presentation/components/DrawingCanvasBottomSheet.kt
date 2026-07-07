package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.components.DrawingCanvas
import at.techbee.spectacled.screens.core.presentation.components.PathData
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.discard
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.drawing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasBottomSheet(
    initialPathData: List<PathData>?,
    replaceAttachmentUid: String?,
    onDrawingUpdated: (String?, List<PathData>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {

    val pathData = remember { mutableStateListOf<PathData>().apply { initialPathData?.let { addAll(it) } } }

    BottomSheetWithMenu(
        sheetState = sheetState,
        onDismiss = { onDismiss() },
        headline = stringResource(Res.string.drawing),
        menuActionRight = {
            TextButton(
                onClick = {
                    onDrawingUpdated(replaceAttachmentUid, pathData)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.done))
            }
        },
        menuActionLeft = {
            TextButton(
                onClick = { onDismiss() },
            ) {
                Text(stringResource(Res.string.discard))
            }
        },

    ) {

        DrawingCanvas(
            paths = pathData,
            onAddPath = { pathData.add(it) },
            onRemovePaths = { pathData.removeAll(it) },   // TODO
            modifier = Modifier
                .fillMaxWidth()
                .height(800.dp)
                .padding(8.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun DrawingCanvasBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DrawingCanvasBottomSheet(
                initialPathData = null,
                replaceAttachmentUid = null,
                onDrawingUpdated = { _, _ -> },
                onDismiss = { }
            )
        }
    }
}
