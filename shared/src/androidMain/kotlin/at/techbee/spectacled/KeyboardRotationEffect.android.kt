package at.techbee.spectacled

import androidx.compose.runtime.Composable

@Composable
actual fun DismissKeyboardOnRotationEffect() {
    // Android resets the IME window-insets correctly on rotation; nothing to do here.
}
