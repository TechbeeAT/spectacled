package at.techbee.spectacled.screens.core

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.input.PlatformImeOptions
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val platform: Platforms = Platforms.IOS
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@OptIn(ExperimentalComposeUiApi::class)
actual fun nativeImeOptions(): PlatformImeOptions? = PlatformImeOptions {
    usingNativeTextInput(true)
}