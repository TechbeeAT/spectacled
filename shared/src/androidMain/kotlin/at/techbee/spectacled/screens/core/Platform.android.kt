package at.techbee.spectacled.screens.core

import android.os.Build
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.shared.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidPlatform : Platform {
    override val platform: Platforms = Platforms.ANDROID // = "Android ${Build.VERSION.SDK_INT}"
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

fun SpectacledVariant.getAndroidLogoResId(): Int {
    return when (this) {
        SpectacledVariant.JOURNALS -> R.drawable.logo_spectacled_journals_android
        SpectacledVariant.NOTES -> R.drawable.logo_spectacled_notes_android
        SpectacledVariant.TASKS -> R.drawable.logo_spectacled_tasks_android
    }
}

actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO