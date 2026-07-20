package at.techbee.spectacled.notes

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.window.OnFocusBehavior

fun MainViewController() = ComposeUIViewController(
    // Don't let Compose translate the whole scene up to keep the focused field above the
    // keyboard (that pushes the top app bar off-screen on iOS). The screens already reserve
    // space for the keyboard via imePadding(), so leave the scene in place.
    configure = { onFocusBehavior = OnFocusBehavior.DoNothing }
) { NotesApp() }
