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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasBottomSheet(
    //initialUrl: Url?,
    //onUrlEdited: (Url?) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {

    BottomSheetWithMenu(
        sheetState = sheetState,
        onDismiss = { onDismiss() },
        headline = "Add/edit drawing\n(work in progress)",// stringResource(Res.string.add_edit_url),
        menuAction = {
            TextButton(
                onClick = {
                }
            ) {
                //Text(stringResource(Res.string.delete))
                Text("TODO")
            }
        }
    ) {

        val pathData = remember { mutableStateListOf<PathData>() }

        DrawingCanvas(
            paths = pathData,
            onAddPath = { pathData.add(it) },  // TODO
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
                onDismiss = { }
            )
        }
    }
}
