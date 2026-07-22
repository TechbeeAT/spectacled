package at.techbee.spectacled.tasks

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController

// DoNothing prevents Compose's default FocusableAboveKeyboard from additionally lifting the whole view and compensating twice.
fun MainViewController() = ComposeUIViewController(
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing }
) { TasksApp() }
