package at.techbee.spectacled.screens.core

import androidx.compose.ui.text.input.PlatformImeOptions

class JVMPlatform: Platform {
    override val platform: Platforms = Platforms.DESKTOP
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun nativeImeOptions(): PlatformImeOptions? = null