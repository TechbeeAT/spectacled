package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetWithMenu(
    sheetState: SheetState = rememberModalBottomSheetState(),
    menuAction: @Composable () -> Unit = { },
    showLoadingIndicator: Boolean = false,
    gesturesEnabled: Boolean = true,
    allowClose: Boolean = true,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {

    val header = @Composable {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(8.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { onDismiss() },
                    enabled = allowClose
                ) {
                    Text(stringResource(Res.string.close))
                }
            }


            if (gesturesEnabled) {
                BottomSheetDefaults.DragHandle()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth()
            ) {

                AnimatedVisibility(showLoadingIndicator) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
                menuAction()
            }
        }
    }

    if (LocalInspectionMode.current) {
        Column(modifier = Modifier.fillMaxWidth()) {
            header()
            content()
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            dragHandle = header,
            sheetGesturesEnabled = gesturesEnabled
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun BottomSheetWithMenu_Preview() {

    BottomSheetWithMenu(
        sheetState = rememberModalBottomSheetState(),
        showLoadingIndicator = true,
        menuAction = {
            TextButton(
                onClick = {  },
            ) {
                Text("Action")
            }

        },
        onDismiss = {}
    ) {
        Column(modifier = Modifier.padding(16.dp)){
            Text("Sample content")
        }
    }
}
