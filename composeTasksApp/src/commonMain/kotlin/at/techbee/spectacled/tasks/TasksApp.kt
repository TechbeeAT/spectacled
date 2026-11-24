package at.techbee.spectacled.tasks

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import at.techbee.spectacled.SpectacledApp
import at.techbee.spectacled.SpectacledVariant


@Composable
@Preview
fun TasksApp(
    onCloseApp: () -> Unit = {}
) = SpectacledApp(
    spectacledVariant = SpectacledVariant.TASKS,
    onCloseApp = onCloseApp
)
