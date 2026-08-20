package at.techbee.spectacled.screens.core.presentation

import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.getPlatform

/**
 * Applies [imePadding] on every platform except iOS.
 *
 * On iOS keyboard avoidance is handled natively by SwiftUI: the hosting `ComposeView` no longer
 * ignores the `.keyboard` safe area, so SwiftUI resizes the Compose view above the keyboard and
 * resets it reliably on rotation. Applying [imePadding] there as well would compensate twice, and
 * CMP's own `WindowInsets.ime` can stay stuck at its pre-rotation height after a rotation, so it is
 * skipped entirely on iOS.
 */
fun Modifier.imeAwarePadding(): Modifier =
    if (getPlatform().platform == Platforms.IOS) this else this.imePadding()
