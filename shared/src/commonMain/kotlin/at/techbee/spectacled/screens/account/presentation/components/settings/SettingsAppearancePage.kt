package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.theme.ThemeFont
import at.techbee.spectacled.theme.ThemeOption
import com.materialkolor.PaletteStyle
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.settings
import spectacled.shared.generated.resources.theme
import spectacled.shared.generated.resources.theme_amoled
import spectacled.shared.generated.resources.theme_dynamic_colors
import spectacled.shared.generated.resources.theme_font
import spectacled.shared.generated.resources.theme_palette_style


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearancePage(
    userAppPreferencesStore: UserAppPreferencesStore,
    modifier: Modifier = Modifier
) {

    var themeOptionDropdownExpanded by remember { mutableStateOf(false) }
    val themeOption by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(userAppPreferencesStore.themeOption)

    var themePaletteStyleDropdownExpanded by remember { mutableStateOf(false) }
    val themePaletteStyle by userAppPreferencesStore.getThemePaletteStlyeAsFlow().collectAsState(userAppPreferencesStore.themePaletteStlye)

    val themeDynamicColorsEnabledBoolean by userAppPreferencesStore.getThemeDynamicColorsEnabledAsFlow().collectAsState(userAppPreferencesStore.themeDynamicColorsEnabled)
    val themeAmoledBoolean by userAppPreferencesStore.getThemeAmoledAsFlow().collectAsState(userAppPreferencesStore.themeAmoled)

    var themeFontDropDownExpanded by remember { mutableStateOf(false) }
    val themeFont by userAppPreferencesStore.getThemeFontAsFlow().collectAsState(userAppPreferencesStore.themeFont)


    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = stringResource(Res.string.settings),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        AssistChip(
            onClick = { themeOptionDropdownExpanded = true },
            label = {

                Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(Res.string.theme),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(stringResource(themeOption.stringRes))
                }

                DropdownMenu(
                    expanded = themeOptionDropdownExpanded,
                    onDismissRequest = { themeOptionDropdownExpanded = false },
                ) {

                    ThemeOption.entries.forEach { themeOption ->
                        DropdownMenuItem(
                            text = { Text(stringResource(themeOption.stringRes)) },
                            onClick = {
                                userAppPreferencesStore.themeOption = themeOption
                                themeOptionDropdownExpanded = false
                            },
                        )
                    }
                }
            },
            leadingIcon = { Icon(Icons.Outlined.FormatPaint, null) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
            modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
        )

        if (getPlatform().platform == Platforms.ANDROID) {
            AssistChip(
                onClick = { userAppPreferencesStore.themeDynamicColorsEnabled = !themeDynamicColorsEnabledBoolean },
                label = {
                    Box(
                        modifier = Modifier.padding(vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = stringResource(Res.string.theme_dynamic_colors))
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.Colorize, null) },
                trailingIcon = {
                    Switch(
                        checked = themeDynamicColorsEnabledBoolean,
                        onCheckedChange = { userAppPreferencesStore.themeDynamicColorsEnabled = it }
                    )
                },
                modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
            )
        }

        AnimatedVisibility(!themeDynamicColorsEnabledBoolean) {
            AssistChip(
                onClick = { themePaletteStyleDropdownExpanded = true },
                label = {

                    Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(Res.string.theme_palette_style),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(themePaletteStyle.name)
                    }

                    DropdownMenu(
                        expanded = themePaletteStyleDropdownExpanded,
                        onDismissRequest = { themePaletteStyleDropdownExpanded = false },
                    ) {

                        PaletteStyle.entries.forEach { paletteStyle ->
                            DropdownMenuItem(
                                text = { Text(paletteStyle.name) },
                                onClick = {
                                    userAppPreferencesStore.themePaletteStlye = paletteStyle
                                    themePaletteStyleDropdownExpanded = false
                                },
                            )
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.ColorLens, null) },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
            )
        }

        AnimatedVisibility(!themeDynamicColorsEnabledBoolean && themeOption != ThemeOption.LIGHT) {
            AssistChip(
                onClick = { userAppPreferencesStore.themeAmoled = !themeAmoledBoolean },
                label = {
                    Box(
                        modifier = Modifier.padding(vertical = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = stringResource(Res.string.theme_amoled))
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.ModeNight, null) },
                trailingIcon = {
                    Switch(
                        checked = themeAmoledBoolean,
                        onCheckedChange = { userAppPreferencesStore.themeAmoled = it }
                    )
                },
                modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
            )
        }

        AssistChip(
            onClick = { themeFontDropDownExpanded = true },
            label = {

                Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(Res.string.theme_font),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(themeFont.fontName)
                }

                DropdownMenu(
                    expanded = themeFontDropDownExpanded,
                    onDismissRequest = { themeFontDropDownExpanded = false },
                ) {

                    ThemeFont.entries.forEach { themeFont ->
                        DropdownMenuItem(
                            text = { Text(themeFont.fontName) },
                            onClick = {
                                userAppPreferencesStore.themeFont = themeFont
                                themeFontDropDownExpanded = false
                            },
                        )
                    }
                }
            },
            leadingIcon = { Icon(Icons.Outlined.FontDownload, null) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
            modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SettingsAppearancePage_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            SettingsAppearancePage(
                UserAppPreferencesStore.getEmptyPreferenceStoreForPreview(SpectacledVariant.JOURNALS)
            )
        }
    }
}
