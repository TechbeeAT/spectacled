package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add
import spectacled.shared.generated.resources.add_alarm
import spectacled.shared.generated.resources.cancel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmBottomSheet(
    onAlarmConfirmed: (Alarm) -> Unit,
    onDismiss: () -> Unit
) {


/*
    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        focusRequester.requestFocus()
    }

 */

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        headline = stringResource(Res.string.add_alarm),
        menuActionRight = {
            TextButton(
                onClick = {
                    TODO()  // onAlarmConfirmed()
                    onDismiss()
                },
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        menuActionLeft = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    ) {

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Text("TODO")
        }
    }
}


@Preview
@Composable
private fun AddAlarmBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            AddAlarmBottomSheet(
                onAlarmConfirmed = { },
                onDismiss = { }
            )
        }
    }
}
