package at.techbee.spectacled.screens.core

import androidx.compose.ui.text.input.PlatformImeOptions


enum class Platforms { ANDROID, IOS, DESKTOP, WASM }

interface Platform {
    val platform: Platforms
    val name: String

    fun isIos() = platform == Platforms.IOS
}

expect fun getPlatform(): Platform

expect fun nativeImeOptions(): PlatformImeOptions?