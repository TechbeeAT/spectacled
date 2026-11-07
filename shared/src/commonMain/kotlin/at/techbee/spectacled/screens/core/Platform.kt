package at.techbee.spectacled.screens.core

import at.techbee.spectacled.shared.BuildKonfig
import kotlinx.coroutines.CoroutineDispatcher


enum class Platforms { ANDROID, IOS, DESKTOP, WASM }

interface Platform {
    val platform: Platforms
    val name: String
}

expect fun getPlatform(): Platform

expect val ioDispatcher: CoroutineDispatcher

fun buildIsPrerelease() = listOf("-internal", "-alpha", "-beta").any {
    BuildKonfig.APP_VERSION_STRING.contains(it)
}