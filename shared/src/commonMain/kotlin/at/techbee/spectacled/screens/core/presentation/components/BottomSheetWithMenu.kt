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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.imeAwarePadding
import at.techbee.spectacled.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetWithMenu(
    sheetState: SheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded),
    headline: String? = null,
    menuActionLeft: @Composable () -> Unit = { },
    menuActionRight: @Composable () -> Unit = { },
    showLoadingIndicator: Boolean = false,
    gesturesEnabled: Boolean = true,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                menuActionLeft()
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
                menuActionRight()
            }
        }
    }


    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = sheetState,
        dragHandle = header,
        sheetGesturesEnabled = gesturesEnabled
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            // Keeps text fields in sheets visible above the keyboard. On iOS this is a no-op
            // because SwiftUI resizes the view for the keyboard natively (see imeAwarePadding).
            modifier = modifier.imeAwarePadding()
        ) {

            headline?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            content()
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun BottomSheetWithMenu_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {

            BottomSheetWithMenu(
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded),
                showLoadingIndicator = true,
                menuActionRight = {
                    TextButton(
                        onClick = { },
                    ) {
                        Text("Action")
                    }

                },

                onDismiss = {}
            ) {
                Text("Sample content")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun BottomSheetWithMenu_Headline_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {

            BottomSheetWithMenu(
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded),
                headline = "Headline",
                showLoadingIndicator = true,
                menuActionRight = {
                    TextButton(
                        onClick = { },
                    ) {
                        Text("Action")
                    }

                },
                onDismiss = {}
            ) {
                Text("Sample content")
            }
        }
    }
}
