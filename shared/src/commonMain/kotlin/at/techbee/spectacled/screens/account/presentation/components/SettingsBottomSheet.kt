package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.AiSettingsStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.theme.ThemeFont
import at.techbee.spectacled.theme.ThemeOption
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.ai_api_key_get_key
import spectacled.shared.generated.resources.ai_api_key_helper
import spectacled.shared.generated.resources.ai_api_key_label
import spectacled.shared.generated.resources.ai_api_key_save
import spectacled.shared.generated.resources.ai_extract_settings_title
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.close
import spectacled.shared.generated.resources.ic_cognition
import spectacled.shared.generated.resources.remove
import spectacled.shared.generated.resources.settings
import spectacled.shared.generated.resources.show_hide_password
import spectacled.shared.generated.resources.theme
import spectacled.shared.generated.resources.theme_amoled
import spectacled.shared.generated.resources.theme_dynamic_colors
import spectacled.shared.generated.resources.theme_font
import spectacled.shared.generated.resources.theme_palette_style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    sheetState: SheetState,
    userAppPreferencesStore: UserAppPreferencesStore,
    aiSettingsStore: AiSettingsStore,
    onDismiss: () -> Unit,
) {

    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val apiKeyState = rememberTextFieldState()
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var apiKeySaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val storedApiKey = aiSettingsStore.loadAnthropicApiKey()
        if (!storedApiKey.isNullOrBlank()) {
            apiKeyState.setTextAndPlaceCursorAtEnd(storedApiKey)
            apiKeySaved = true
        }
    }

    var themeOptionDropdownExpanded by remember { mutableStateOf(false) }
    val themeOption by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(userAppPreferencesStore.themeOption)

    var themePaletteStyleDropdownExpanded by remember { mutableStateOf(false) }
    val themePaletteStyle by userAppPreferencesStore.getThemePaletteStlyeAsFlow().collectAsState(userAppPreferencesStore.themePaletteStlye)

    val themeDynamicColorsEnabledBoolean by userAppPreferencesStore.getThemeDynamicColorsEnabledAsFlow().collectAsState(userAppPreferencesStore.themeDynamicColorsEnabled)
    val themeAmoledBoolean by userAppPreferencesStore.getThemeAmoledAsFlow().collectAsState(userAppPreferencesStore.themeAmoled)

    var themeFontDropDownExpanded by remember { mutableStateOf(false) }
    val themeFont by userAppPreferencesStore.getThemeFontAsFlow().collectAsState(userAppPreferencesStore.themeFont)

    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
        headline = stringResource(Res.string.settings),
        sheetState = sheetState,
        gesturesEnabled = false,
        menuActionLeft = { },
        menuActionRight = {
            TextButton(
                onClick = { onDismiss() },
            ) {
                Text(stringResource(Res.string.close))
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

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
                modifier = Modifier.widthIn(min = 350.dp)
            )

            if(getPlatform().platform == Platforms.ANDROID) {
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
                    modifier = Modifier.widthIn(min = 350.dp)
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
                    modifier = Modifier.widthIn(min = 350.dp)
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
                    modifier = Modifier.widthIn(min = 350.dp)
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
                modifier = Modifier.widthIn(min = 350.dp)
            )

            HorizontalDivider(modifier = Modifier.widthIn(min = 350.dp).padding(vertical = 8.dp))

            Column(modifier = Modifier.widthIn(min = 350.dp)) {

                Text(
                    text = stringResource(Res.string.ai_extract_settings_title),
                    style = MaterialTheme.typography.labelSmall
                )

                OutlinedSecureTextField(
                    state = apiKeyState,
                    label = { Text(stringResource(Res.string.ai_api_key_label)) },
                    textObfuscationMode = if (isApiKeyVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_cognition), null) },
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Crossfade(isApiKeyVisible) { visible ->
                                if (visible) Icon(
                                    Icons.Outlined.Visibility,
                                    contentDescription = stringResource(Res.string.show_hide_password)
                                ) else Icon(
                                    Icons.Outlined.VisibilityOff,
                                    contentDescription = stringResource(Res.string.show_hide_password)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(Res.string.ai_api_key_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { uriHandler.openUri("https://console.anthropic.com/settings/keys") }) {
                        Text(stringResource(Res.string.ai_api_key_get_key))
                    }
                    if (apiKeySaved) {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                aiSettingsStore.saveAnthropicApiKey(null)
                                apiKeyState.setTextAndPlaceCursorAtEnd("")
                                apiKeySaved = false
                            }
                        }) {
                            Text(stringResource(Res.string.remove))
                        }
                    }
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val newApiKey = apiKeyState.text.toString().trim()
                                aiSettingsStore.saveAnthropicApiKey(newApiKey.ifBlank { null })
                                apiKeySaved = newApiKey.isNotBlank()
                            }
                        }
                    ) {
                        Text(stringResource(Res.string.ai_api_key_save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SettingsBottomSheet_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            SettingsBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                userAppPreferencesStore = object: UserAppPreferencesStore {
                    override fun save(key: String, value: String) { }
                    override fun load(key: String): String? { return null }
                    override fun loadAsFlow(key: String): Flow<String?> { return flowOf(null ) }
                    override fun remove(key: String) { }
                },
                aiSettingsStore = object: AiSettingsStore {
                    override suspend fun saveAnthropicApiKey(apiKey: String?) { }
                    override suspend fun loadAnthropicApiKey(): String? { return null }
                },
                onDismiss = {}
            )
        }
    }

}
