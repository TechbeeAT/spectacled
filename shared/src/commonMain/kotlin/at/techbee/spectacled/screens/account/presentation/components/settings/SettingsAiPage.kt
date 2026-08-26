package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.ai.AiProvider
import at.techbee.spectacled.screens.core.data.ai.ClaudeModel
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.ai_model
import spectacled.shared.generated.resources.ai_provider
import spectacled.shared.generated.resources.anthropic_api_key
import spectacled.shared.generated.resources.anthropic_api_key_info
import spectacled.shared.generated.resources.get_an_api_key
import spectacled.shared.generated.resources.ic_ai_model
import spectacled.shared.generated.resources.ic_ai_server
import spectacled.shared.generated.resources.ic_passkey
import spectacled.shared.generated.resources.openai_api_key
import spectacled.shared.generated.resources.openai_base_url
import spectacled.shared.generated.resources.openai_info
import spectacled.shared.generated.resources.show_hide_password


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAiPage(
    userAppPreferencesStore: UserAppPreferencesStore,
    modifier: Modifier = Modifier
) {

    val uriHandler = LocalUriHandler.current

    var aiProviderDropdownExpanded by remember { mutableStateOf(false) }
    val aiProvider by userAppPreferencesStore.getAiProviderAsFlow().collectAsState(userAppPreferencesStore.aiProvider)

    var claudeModelDropdownExpanded by remember { mutableStateOf(false) }
    val claudeModel by userAppPreferencesStore.getClaudeModelAsFlow().collectAsState(userAppPreferencesStore.claudeModel)

    var isClaudeUserApiKeyVisible by remember { mutableStateOf(false) }
    val claudeUserApiKeyState = rememberTextFieldState(userAppPreferencesStore.claudeUserApiKey ?: "")
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
    val openAiApiKeyState = rememberTextFieldState(userAppPreferencesStore.openAiApiKey ?: "")
    LaunchedEffect(openAiApiKeyState.text) {
        userAppPreferencesStore.openAiApiKey = openAiApiKeyState.text.toString()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = stringResource(Res.string.ai_provider),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
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
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_ai_model),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null) },
                    modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                )

                OutlinedSecureTextField(
                    state = claudeUserApiKeyState,
                    label = { Text(stringResource(Res.string.anthropic_api_key)) },
                    textObfuscationMode = if (isClaudeUserApiKeyVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_passkey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
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
                )
            }
        }

        AnimatedVisibility(aiProvider == AiProvider.OPENAI_COMPATIBLE) {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    state = openAiBaseUrlState,
                    label = { Text(stringResource(Res.string.openai_base_url)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_ai_server),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    placeholder = { Text("http://localhost:11434") },
                    modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                )

                OutlinedTextField(
                    state = openAiModelState,
                    label = { Text(stringResource(Res.string.ai_model)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_ai_model),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    placeholder = { Text("llama3.2") },
                    modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
                )

                OutlinedSecureTextField(
                    state = openAiApiKeyState,
                    label = { Text(stringResource(Res.string.openai_api_key)) },
                    textObfuscationMode = if (isOpenAiApiKeyVisible) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_passkey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
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
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SettingsAiPage_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            SettingsAiPage(
                userAppPreferencesStore = UserAppPreferencesStore.getEmptyPreferenceStoreForPreview(SpectacledVariant.JOURNALS),
            )
        }
    }
}
