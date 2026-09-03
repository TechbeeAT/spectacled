package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.insecure_connection_warning
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.settings_proxy_hosted_active_message
import spectacled.shared.generated.resources.settings_proxy_hosted_active_title
import spectacled.shared.generated.resources.settings_proxy_hosted_review
import spectacled.shared.generated.resources.settings_proxy_hosted_switch_to_own
import spectacled.shared.generated.resources.settings_proxy_option_hosted
import spectacled.shared.generated.resources.settings_proxy_option_hosted_badge
import spectacled.shared.generated.resources.settings_proxy_option_hosted_info
import spectacled.shared.generated.resources.settings_proxy_option_own
import spectacled.shared.generated.resources.settings_proxy_option_own_info
import spectacled.shared.generated.resources.settings_proxy_option_own_recommended
import spectacled.shared.generated.resources.settings_proxy_server
import spectacled.shared.generated.resources.settings_proxy_server_info
import spectacled.shared.generated.resources.settings_proxy_preset_local_development
import spectacled.shared.generated.resources.settings_proxy_presets
import spectacled.shared.generated.resources.settings_proxy_setup_instructions


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMorePage(
    userAppPreferencesStore: UserAppPreferencesStore,
    modifier: Modifier = Modifier
) {

    val userProxyServer by userAppPreferencesStore.getUserProxyServerAsFlow().collectAsState(userAppPreferencesStore.userProxyServer)
    val hostedProxyConsentUrl by userAppPreferencesStore.getHostedProxyConsentUrlAsFlow().collectAsState(userAppPreferencesStore.hostedProxyConsentUrl)

    val hostedProxyUrl = HttpClientFactory.HOSTED_WEB_PROXY_URL
    val hostedProxySelected = userProxyServer?.trim() == hostedProxyUrl

    // Kept around while the hosted proxy is selected so switching back restores what the user had typed.
    var ownProxyServerDraft by remember { mutableStateOf(userProxyServer?.takeIf { it.trim() != hostedProxyUrl } ?: "") }
    var trustDialogVisible by remember { mutableStateOf(false) }
    var proxyPresetsExpanded by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    fun selectHostedProxy() {
        // Consent is per URL: an instance the user never agreed to always asks first.
        if (hostedProxyConsentUrl == hostedProxyUrl)
            userAppPreferencesStore.userProxyServer = hostedProxyUrl
        else
            trustDialogVisible = true
    }

    fun selectOwnProxy() {
        userAppPreferencesStore.userProxyServer = ownProxyServerDraft.ifBlank { null }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = stringResource(Res.string.more),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (getPlatform().platform == Platforms.WASM || LocalInspectionMode.current) {

            Text(
                text = stringResource(Res.string.settings_proxy_server),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
            )
            Text(
                text = stringResource(Res.string.settings_proxy_server_info),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth()
            )

            TextButton(onClick = { uriHandler.openUri(HttpClientFactory.PROXY_SETUP_INFO_URL) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(Res.string.settings_proxy_setup_instructions))
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
                }
            }

            ProxyOptionCard(
                selected = !hostedProxySelected,
                icon = Icons.Outlined.Dns,
                title = stringResource(Res.string.settings_proxy_option_own),
                info = stringResource(Res.string.settings_proxy_option_own_info),
                badge = stringResource(Res.string.settings_proxy_option_own_recommended),
                onClick = { selectOwnProxy() }
            )

            AnimatedVisibility(!hostedProxySelected) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth().padding(start = 16.dp)
                ) {

                    OutlinedTextField(
                        value = ownProxyServerDraft,
                        onValueChange = {
                            ownProxyServerDraft = it
                            userAppPreferencesStore.userProxyServer = it.ifBlank { null }
                        },
                        placeholder = { Text("https://") },
                        supportingText = {
                            AnimatedVisibility(ownProxyServerDraft.trim().startsWith("http://")) {
                                Text(
                                    text = stringResource(Res.string.insecure_connection_warning),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        label = { Text(stringResource(Res.string.settings_proxy_server)) },
                        // Presets for this field only - the hosted proxy is deliberately not among them,
                        // since selecting it has to go through the consent dialog.
                        trailingIcon = {
                            IconButton(onClick = { proxyPresetsExpanded = !proxyPresetsExpanded }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(Res.string.settings_proxy_presets)
                                )

                                DropdownMenu(
                                    expanded = proxyPresetsExpanded,
                                    onDismissRequest = { proxyPresetsExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(stringResource(Res.string.settings_proxy_preset_local_development))
                                                Text(
                                                    text = HttpClientFactory.DEFAULT_WEB_PROXY_URL,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        },
                                        onClick = {
                                            ownProxyServerDraft = HttpClientFactory.DEFAULT_WEB_PROXY_URL
                                            userAppPreferencesStore.userProxyServer = HttpClientFactory.DEFAULT_WEB_PROXY_URL
                                            proxyPresetsExpanded = false
                                        }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            ProxyOptionCard(
                selected = hostedProxySelected,
                icon = Icons.Outlined.Cloud,
                title = stringResource(Res.string.settings_proxy_option_hosted),
                info = stringResource(Res.string.settings_proxy_option_hosted_info),
                badge = stringResource(Res.string.settings_proxy_option_hosted_badge),
                badgeColor = MaterialTheme.colorScheme.error,
                supportingText = hostedProxyUrl,
                onClick = { selectHostedProxy() }
            )

            // While the hosted proxy is in use the disclosure stays on screen - consent is given once,
            // but the user should never have to remember what they agreed to.
            AnimatedVisibility(hostedProxySelected) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth().padding(start = 16.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Warning, null)
                            Text(
                                text = stringResource(Res.string.settings_proxy_hosted_active_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = stringResource(Res.string.settings_proxy_hosted_active_message, hostedProxyUrl),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val buttonColors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(
                                onClick = { trustDialogVisible = true },
                                colors = buttonColors
                            ) {
                                Text(stringResource(Res.string.settings_proxy_hosted_review))
                            }
                            TextButton(
                                onClick = { selectOwnProxy() },
                                colors = buttonColors
                            ) {
                                Text(stringResource(Res.string.settings_proxy_hosted_switch_to_own))
                            }
                        }
                    }
                }
            }
        }
    }

    if (trustDialogVisible) {
        ProxyTrustDialog(
            proxyUrl = hostedProxyUrl,
            initiallyAccepted = hostedProxySelected,
            onConfirm = {
                userAppPreferencesStore.hostedProxyConsentUrl = hostedProxyUrl
                userAppPreferencesStore.userProxyServer = hostedProxyUrl
                trustDialogVisible = false
            },
            onDismiss = { trustDialogVisible = false },
            onOpenSelfHostingInfo = { uriHandler.openUri(HttpClientFactory.PROXY_SETUP_INFO_URL) }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxyOptionCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    info: String,
    badge: String,
    onClick: () -> Unit,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor =
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.widthIn(min = 350.dp).fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor
                    )
                }
                Text(
                    text = info,
                    style = MaterialTheme.typography.bodySmall
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun SettingsMorePage_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            SettingsMorePage(
                userAppPreferencesStore = UserAppPreferencesStore.getEmptyPreferenceStoreForPreview(SpectacledVariant.JOURNALS)
            )
        }
    }
}
