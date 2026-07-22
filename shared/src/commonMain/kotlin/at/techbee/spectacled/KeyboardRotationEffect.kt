package at.techbee.spectacled

import androidx.compose.runtime.Composable

/**
 * Makes sure the soft keyboard is fully dismissed when the device rotates so the platform's
 * IME window-inset is reset back to zero.
 *
 * This is a no-op on every platform except iOS. On iOS the IME inset that drives
 * [androidx.compose.foundation.layout.imePadding] can stay frozen at its pre-rotation height
 * when the device is rotated while the keyboard is open, leaving a phantom gap at the bottom of
 * the screen even though the keyboard is already gone. Android resets the inset correctly by
 * itself, and desktop/web do not rotate.
 */
@Composable
expect fun DismissKeyboardOnRotationEffect()
