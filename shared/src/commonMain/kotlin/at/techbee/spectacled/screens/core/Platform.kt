package at.techbee.spectacled.screens.core


enum class Platforms { ANDROID, IOS, DESKTOP, WASM }

interface Platform {
    val platform: Platforms
    val name: String
}

expect fun getPlatform(): Platform
