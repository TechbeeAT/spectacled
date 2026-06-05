package at.techbee.spectacled.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.theme_dark
import spectacled.shared.generated.resources.theme_light
import spectacled.shared.generated.resources.theme_system


@Composable
fun getThemeForSeedColor(color: Color?): ColorScheme {

    val userAppPreferencesStore =
        if(LocalInspectionMode.current)   // only to make previews work
            object: UserAppPreferencesStore {
                override fun save(key: String, value: String) {}
                override fun load(key: String): String? {return null }
                override fun loadAsFlow(key: String): Flow<String?> { return flowOf(null) }
                override fun remove(key: String) {}
            }
        else
            koinInject<PlatformUserAppPreferencesStore>()


    val themeOption by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(userAppPreferencesStore.themeOption)
    val overrideIsDark = when(themeOption) {
        ThemeOption.SYSTEM -> null
        ThemeOption.LIGHT -> false
        ThemeOption.DARK -> true
    }

    val themePaletteStyle by userAppPreferencesStore.getThemePaletteStlyeAsFlow().collectAsState(userAppPreferencesStore.themePaletteStlye)
    val themeAmoledBoolean by userAppPreferencesStore.getThemeAmoledAsFlow().collectAsState(userAppPreferencesStore.themeAmoled)

    return if (color == null)
        MaterialTheme.colorScheme
    else
        dynamicColorScheme(
            primary = color,
            isDark = overrideIsDark ?: isSystemInDarkTheme(),
            style = themePaletteStyle,
            isAmoled = themeAmoledBoolean,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        )
}

@Composable
expect fun AppTheme(
    spectacledVariant: SpectacledVariant,
    content: @Composable () -> Unit
)

enum class ThemeOption(
    val stringRes: StringResource
) {
    SYSTEM(Res.string.theme_system),
    LIGHT(Res.string.theme_light),
    DARK(Res.string.theme_dark);

    companion object {
        fun fromString(value: String?) = entries.find { it.name == value } ?: SYSTEM
    }
}