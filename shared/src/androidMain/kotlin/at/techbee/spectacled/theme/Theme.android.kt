package at.techbee.spectacled.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext


@Composable
actual fun getAndroidDynamicColorScheme(themeOption: ThemeOption): ColorScheme? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val isDark = when (themeOption) {
            ThemeOption.SYSTEM -> isSystemInDarkTheme()
            ThemeOption.LIGHT -> false
            ThemeOption.DARK -> true
        }
        if (isDark)
            dynamicDarkColorScheme(context)
        else
            dynamicLightColorScheme(context)
    } else {
        null
    }
}