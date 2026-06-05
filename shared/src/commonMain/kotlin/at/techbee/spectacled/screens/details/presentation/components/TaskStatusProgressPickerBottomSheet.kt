package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.status_no_status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskStatusProgressPickerBottomSheet(
    status: Status?,
    percentComplete: Long?,
    sheetState: SheetState,
    onProgressUpdated: (Long) -> Unit,
    onStatusUpdated: (Status?) -> Unit,
    onDismiss: () -> Unit
) {

    BottomSheetWithMenu(
        sheetState = sheetState,
        onDismiss = { onDismiss() },
        menuAction = { TextButton(onClick = {
            onStatusUpdated(null)
            onDismiss()
        }) { Text(stringResource(Res.string.status_no_status)) } }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp, start = 16.dp)
            ) {
                Text("$percentComplete %")

                Slider(
                    value = percentComplete?.toFloat()?:0F,
                    valueRange = 0f..100f,
                    //intRangeSteps = 100,
                    //steps = 20,
                    onValueChange = { newPercent -> onProgressUpdated(newPercent.toLong()) },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                )

                TriStateCheckbox(
                    state = when(percentComplete) {
                        0L -> ToggleableState.Off
                        in 1L..99L -> ToggleableState.Indeterminate
                        100L -> ToggleableState.On
                        else -> ToggleableState.Indeterminate
                    },
                    onClick = { onProgressUpdated(if(percentComplete == 100L) 0L else 100L) }
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                val statusSet = setOf(Status.NEEDS_ACTION, Status.IN_PROCESS, Status.COMPLETED, Status.CANCELLED)

                statusSet.forEach { selectableStatus ->
                    FilterChip(
                        selected = selectableStatus == status,
                        onClick = { onStatusUpdated(selectableStatus) },
                        label = { Text(stringResource(selectableStatus.stringRes)) }
                    )
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun TaskStatusProgressPickerBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Surface {
            TaskStatusProgressPickerBottomSheet(
                status = null,
                percentComplete = 0L,
                sheetState = rememberModalBottomSheetState(),
                onStatusUpdated = {},
                onProgressUpdated = {},
                onDismiss = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun TaskStatusProgressPickerBottomSheet_inprocess_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Surface {
            TaskStatusProgressPickerBottomSheet(
                status = Status.IN_PROCESS,
                percentComplete = 33,
                sheetState = rememberModalBottomSheetState(),
                onStatusUpdated = {},
                onProgressUpdated = {},
                onDismiss = {}
            )
        }
    }
}




