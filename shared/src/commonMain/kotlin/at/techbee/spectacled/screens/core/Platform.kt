package at.techbee.spectacled.screens.core

import kotlinx.coroutines.CoroutineDispatcher


enum class Platforms { ANDROID, IOS, DESKTOP, WASM }

interface Platform {
    val platform: Platforms
    val name: String
}

expect fun getPlatform(): Platform

expect val ioDispatcher: CoroutineDispatcher