package at.techbee.spectacled.notes

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController

// Keyboard avoidance is handled natively by SwiftUI (the hosting ComposeView keeps the
// .keyboard safe area). DoNothing prevents Compose's default FocusableAboveKeyboard from
// additionally lifting the whole view and compensating twice.
fun MainViewController() = ComposeUIViewController(
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing }
) { NotesApp() }
