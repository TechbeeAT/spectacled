package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.percent_complete
import spectacled.shared.generated.resources.priority
import spectacled.shared.generated.resources.priority_Medium
import spectacled.shared.generated.resources.priority_high
import spectacled.shared.generated.resources.priority_low
import spectacled.shared.generated.resources.priority_not_specified
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskStatusProgressPickerBottomSheet(
    status: Status?,
    percentComplete: Long?,
    priority: Long?,
    sheetState: SheetState,
    onProgressUpdated: (Long) -> Unit,
    onStatusUpdated: (Status?) -> Unit,
    onPriorityUpdated: (Long?) -> Unit,
    onDismiss: () -> Unit
) {

    BottomSheetWithMenu(
        sheetState = sheetState,
        onDismiss = { onDismiss() },
        menuActionRight = {
            TextButton(
                onClick = { onDismiss() }
            ) {
                Text(stringResource(Res.string.done))
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp, start = 16.dp)
            ) {
                Text(stringResource(Res.string.percent_complete, (percentComplete ?: 0).toInt()))

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

                FilterChip(
                    leadingIcon = { Status.NO_STATUS.StatusIcon(null) },
                    selected = status == null,
                    onClick = { onStatusUpdated(null) },
                    label = { Text(stringResource(Status.NO_STATUS.stringRes)) }
                )

                val statusSet = setOf(Status.NEEDS_ACTION, Status.IN_PROCESS, Status.COMPLETED, Status.CANCELLED)

                statusSet.forEach { selectableStatus ->
                    FilterChip(
                        leadingIcon = { selectableStatus.StatusIcon(null) },
                        selected = selectableStatus == status,
                        onClick = { onStatusUpdated(selectableStatus) },
                        label = { Text(stringResource(selectableStatus.stringRes)) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {

                Text(stringResource(Res.string.priority))

                val currentPriorityStep = when (priority) {
                    null, 0L -> 0f
                    in 6L..9L -> 1f
                    5L -> 2f
                    in 1L..4L -> 3f
                    else -> 0f
                }

                // Local continuous value for smooth dragging
                var sliderValue by remember(priority) { mutableFloatStateOf(currentPriorityStep) }

                LaunchedEffect(priority) {
                    sliderValue = currentPriorityStep
                }

                Slider(
                    value = sliderValue,
                    valueRange = 0f..3f,
                    steps = 2,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        onPriorityUpdated(
                            when (sliderValue.roundToInt()) {
                                0 -> null
                                1 -> 9L
                                2 -> 5L
                                3 -> 1L
                                else -> null
                            }
                        )

                    },
                    thumb = {
                        AssistChip(
                            onClick = {},
                            label = {
                                    Text(
                                        text = stringResource(when (sliderValue.roundToInt()) {
                                            0 -> Res.string.priority_not_specified
                                            1 -> Res.string.priority_low
                                            2 -> Res.string.priority_Medium
                                            3 -> Res.string.priority_high
                                            else -> Res.string.priority_not_specified
                                        }),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(72.dp)
                                    )
                            }
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
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
                priority = 5,
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded),
                onStatusUpdated = {},
                onProgressUpdated = {},
                onPriorityUpdated = {},
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
        Scaffold {
            TaskStatusProgressPickerBottomSheet(
                status = Status.IN_PROCESS,
                percentComplete = 33,
                priority = null,
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Expanded),
                onStatusUpdated = {},
                onProgressUpdated = {},
                onPriorityUpdated = {},
                onDismiss = {}
            )
        }
    }
}




