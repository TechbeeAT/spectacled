package at.techbee.spectacled.screens.core

import android.os.Build

class AndroidPlatform : Platform {
    override val platform: Platforms = Platforms.ANDROID // = "Android ${Build.VERSION.SDK_INT}"
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
