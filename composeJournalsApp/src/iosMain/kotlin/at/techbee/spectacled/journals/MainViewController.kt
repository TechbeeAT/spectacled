package at.techbee.spectacled.journals

import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController

// The compose code handles the keyboard with imePadding() itself; the default
// FocusableAboveKeyboard would additionally lift the whole view and thereby
// duplicate the keyboard compensation.
fun MainViewController() = ComposeUIViewController(
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing }
) { JournalsApp() }
