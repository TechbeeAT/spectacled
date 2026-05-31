package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun JournalStatusPickerBottomSheet(
    status: Status?,
    sheetState: SheetState,
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                val statusSet = setOf(Status.DRAFT, Status.FINAL, Status.CANCELLED)

                statusSet.forEach { selectableStatus ->
                    FilterChip(
                        leadingIcon = { Icon(selectableStatus.vectorIcon!!, null) },
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
private fun JournalStatusPickerBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Surface {
            JournalStatusPickerBottomSheet(
                status = Status.FINAL,
                sheetState = rememberModalBottomSheetState(),
                onStatusUpdated = {},
                onDismiss = {}
            )
        }
    }
}


