package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.FontDownload
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.ai.AiProvider
import at.techbee.spectacled.screens.core.data.ai.ClaudeModel
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.theme.ThemeFont
import at.techbee.spectacled.theme.ThemeOption
import com.materialkolor.PaletteStyle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.advanced
import spectacled.shared.generated.resources.ai_model
import spectacled.shared.generated.resources.ai_provider
import spectacled.shared.generated.resources.anthropic_api_key
import spectacled.shared.generated.resources.anthropic_api_key_info
import spectacled.shared.generated.resources.close
import spectacled.shared.generated.resources.get_an_api_key
import spectacled.shared.generated.resources.ic_ai_model
import spectacled.shared.generated.resources.ic_ai_server
import spectacled.shared.generated.resources.ic_passkey
import spectacled.shared.generated.resources.insecure_connection_warning
import spectacled.shared.generated.resources.openai_api_key
import spectacled.shared.generated.resources.openai_base_url
import spectacled.shared.generated.resources.openai_info
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
    onDismiss: () -> Unit,
) {

    val uriHandler = LocalUriHandler.current

    var themeOptionDropdownExpanded by remember { mutableStateOf(false) }
    val themeOption by userAppPreferencesStore.getThemeOptionAsFlow().collectAsState(userAppPreferencesStore.themeOption)

    var themePaletteStyleDropdownExpanded by remember { mutableStateOf(false) }
    val themePaletteStyle by userAppPreferencesStore.getThemePaletteStlyeAsFlow().collectAsState(userAppPreferencesStore.themePaletteStlye)

    val themeDynamicColorsEnabledBoolean by userAppPreferencesStore.getThemeDynamicColorsEnabledAsFlow().collectAsState(userAppPreferencesStore.themeDynamicColorsEnabled)
    val themeAmoledBoolean by userAppPreferencesStore.getThemeAmoledAsFlow().collectAsState(userAppPreferencesStore.themeAmoled)

    var themeFontDropDownExpanded by remember { mutableStateOf(false) }
    val themeFont by userAppPreferencesStore.getThemeFontAsFlow().collectAsState(userAppPreferencesStore.themeFont)

    //var advancedSectionExpanded by remember { mutableStateOf(false) }

    var aiProviderDropdownExpanded by remember { mutableStateOf(false) }
    val aiProvider by userAppPreferencesStore.getAiProviderAsFlow().collectAsState(userAppPreferencesStore.aiProvider)

    var claudeModelDropdownExpanded by remember { mutableStateOf(false) }
    val claudeModel by userAppPreferencesStore.getClaudeModelAsFlow().collectAsState(userAppPreferencesStore.claudeModel)

    var isClaudeUserApiKeyVisible by remember { mutableStateOf(false) }
    val claudeUserApiKeyState = rememberTextFieldState(userAppPreferencesStore.claudeUserApiKey?:"")
    LaunchedEffect(claudeUserApiKeyState.text) {
        userAppPreferencesStore.claudeUserApiKey = claudeUserApiKeyState.text.toString()
    }

    val openAiBaseUrlState = rememberTextFieldState(userAppPreferencesStore.openAiBaseUrl ?: "")
    LaunchedEffect(openAiBaseUrlState.text) {
        userAppPreferencesStore.openAiBaseUrl = openAiBaseUrlState.text.toString()
    }
    val openAiModelState = rememberTextFieldState(userAppPreferencesStore.openAiModel ?: "")
    LaunchedEffect(openAiModelState.text) {
        userAppPreferencesStore.openAiModel = openAiModelState.text.toString()
    }
    var isOpenAiApiKeyVisible by remember { mutableStateOf(false) }
    val openAiApiKeyState = rememberTextFieldState(userAppPreferencesStore.openAiApiKey?:"")
    LaunchedEffect(openAiApiKeyState.text) {
        userAppPreferencesStore.openAiApiKey = openAiApiKeyState.text.toString()
    }

    var userProxyServerDropdownExpanded by remember { mutableStateOf(false) }
    val userProxyServer by userAppPreferencesStore.getUserProxyServerAsFlow().collectAsState(userAppPreferencesStore.userProxyServer)


    BottomSheetWithMenu(
        onDismiss = { onDismiss() },
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
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {

            Text(
                text = stringResource(Res.string.settings),
                style = MaterialTheme.typography.titleLarge ,
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

            Text(
                text = stringResource(Res.string.advanced),
                style = MaterialTheme.typography.titleLarge ,
                modifier = Modifier.padding(top = 16.dp)
            )

            AssistChip(
                onClick = { aiProviderDropdownExpanded = true },
                label = {
                    Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)) {
                        Text(
                            text = stringResource(Res.string.ai_provider),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(stringResource(aiProvider.providerNameRes))
                    }

                    DropdownMenu(
                        expanded = aiProviderDropdownExpanded,
                        onDismissRequest = { aiProviderDropdownExpanded = false },
                    ) {
                        AiProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(stringResource(provider.providerNameRes)) },
                                onClick = {
                                    userAppPreferencesStore.aiProvider = provider
                                    aiProviderDropdownExpanded = false
                                },
                            )
                        }
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) },
                trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
            )

            AnimatedVisibility(aiProvider == AiProvider.CLAUDE) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AssistChip(
                        onClick = { claudeModelDropdownExpanded = true },
                        label = {
                            Column(modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)) {
                                Text(
                                    text = stringResource(Res.string.ai_model),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(ClaudeModel.fromId(claudeModel).displayName)
                            }

                            DropdownMenu(
                                expanded = claudeModelDropdownExpanded,
                                onDismissRequest = { claudeModelDropdownExpanded = false },
                            ) {
                                ClaudeModel.entries.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.displayName) },
                                        onClick = {
                                            userAppPreferencesStore.claudeModel = model.id
                                            claudeModelDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        },
                        leadingIcon = { Icon(
                            painter = painterResource(Res.drawable.ic_ai_model),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) },
                        trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                        modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                    )

                OutlinedSecureTextField(
                    state = claudeUserApiKeyState,
                    label = { Text(stringResource(Res.string.anthropic_api_key)) },
                    textObfuscationMode = if (isClaudeUserApiKeyVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                    leadingIcon = { Icon(
                        painter = painterResource(Res.drawable.ic_passkey),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    ) },
                    trailingIcon = {
                        IconButton(onClick = { isClaudeUserApiKeyVisible = !isClaudeUserApiKeyVisible }) {
                            Crossfade(isClaudeUserApiKeyVisible) { visible ->
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
                    supportingText = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(Res.string.anthropic_api_key_info),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { uriHandler.openUri("https://console.anthropic.com/settings/keys") }   // TODO: Replace with affiliate-link?
                            ) {
                                Text(stringResource(Res.string.get_an_api_key))
                            }

                        }
                    },
                    modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                ) }
            }

            AnimatedVisibility(aiProvider == AiProvider.OPENAI_COMPATIBLE) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        state = openAiBaseUrlState,
                        label = { Text(stringResource(Res.string.openai_base_url)) },
                        leadingIcon = { Icon(
                            painter = painterResource(Res.drawable.ic_ai_server),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) },
                        placeholder = { Text("http://localhost:11434") },
                        modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                    )

                    OutlinedTextField(
                        state = openAiModelState,
                        label = { Text(stringResource(Res.string.ai_model)) },
                        leadingIcon = { Icon(
                            painter = painterResource(Res.drawable.ic_ai_model),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) },
                        placeholder = { Text("llama3.2") },
                        modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                    )

                    OutlinedSecureTextField(
                        state = openAiApiKeyState,
                        label = { Text(stringResource(Res.string.openai_api_key)) },
                        textObfuscationMode = if (isOpenAiApiKeyVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                        leadingIcon = { Icon(
                            painter = painterResource(Res.drawable.ic_passkey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        ) },
                        trailingIcon = {
                            IconButton(onClick = { isOpenAiApiKeyVisible = !isOpenAiApiKeyVisible }) {
                                Crossfade(isOpenAiApiKeyVisible) { visible ->
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
                        supportingText = { Text(stringResource(Res.string.openai_info)) },
                        modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                    )
                }
            }


            if(getPlatform().platform == Platforms.WASM || LocalInspectionMode.current) {
                OutlinedTextField(
                    value = userProxyServer ?: "",
                    onValueChange = { userAppPreferencesStore.userProxyServer = it.ifBlank { null } },
                    placeholder = { Text("https://") },
                    supportingText = {
                        val trimmedServer = userProxyServer?.trim() ?: ""
                        val isInsecure = trimmedServer.startsWith("http://")

                        Column {
                            AnimatedVisibility(isInsecure) {
                                Text(
                                    text = stringResource(Res.string.insecure_connection_warning),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text("TODO: Info about proxy server")
                        }
                    },
                    label = { Text("Proxy server") },
                    trailingIcon = {
                        TextButton(
                            onClick = { userProxyServerDropdownExpanded = !userProxyServerDropdownExpanded },
                        ) {
                            Icon(Icons.Outlined.MoreVert, null)

                            DropdownMenu(
                                expanded = userProxyServerDropdownExpanded,
                                onDismissRequest = { userProxyServerDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Development test")
                                            Text("http://0.0.0.0:8088")
                                        }
                                    },
                                    onClick = {
                                        userAppPreferencesStore.userProxyServer = "http://0.0.0.0:8088"
                                        userProxyServerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                )
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
                userAppPreferencesStore = UserAppPreferencesStore.getEmptyPreferenceStoreForPreview(SpectacledVariant.JOURNALS),
                onDismiss = {}
            )
        }
    }
}
