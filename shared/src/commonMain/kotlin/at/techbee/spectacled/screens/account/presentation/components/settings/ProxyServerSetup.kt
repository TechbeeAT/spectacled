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
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.WebProxyPolicy
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.insecure_connection_warning
import spectacled.shared.generated.resources.settings_proxy_hosted_active_message
import spectacled.shared.generated.resources.settings_proxy_hosted_active_title
import spectacled.shared.generated.resources.settings_proxy_hosted_review
import spectacled.shared.generated.resources.settings_proxy_hosted_switch_to_own
import spectacled.shared.generated.resources.settings_proxy_option_direct
import spectacled.shared.generated.resources.settings_proxy_option_direct_badge
import spectacled.shared.generated.resources.settings_proxy_option_direct_info
import spectacled.shared.generated.resources.settings_proxy_option_hosted
import spectacled.shared.generated.resources.settings_proxy_option_hosted_badge
import spectacled.shared.generated.resources.settings_proxy_option_hosted_info
import spectacled.shared.generated.resources.settings_proxy_option_own
import spectacled.shared.generated.resources.settings_proxy_option_own_info
import spectacled.shared.generated.resources.settings_proxy_option_own_recommended
import spectacled.shared.generated.resources.settings_proxy_own_empty_connects_directly
import spectacled.shared.generated.resources.settings_proxy_preset_local_development
import spectacled.shared.generated.resources.settings_proxy_presets
import spectacled.shared.generated.resources.settings_proxy_server
import spectacled.shared.generated.resources.settings_proxy_server_info
import spectacled.shared.generated.resources.settings_proxy_setup_instructions


/** The three ways the web build can reach CalDAV servers; which are offered depends on the origin. */
private enum class ProxyChoice { OWN, HOSTED, DIRECT }

/**
 * Picks the CORS proxy the web build routes CalDAV traffic through: the user's own instance, the
 * one Techbee hosts - only after [ProxyTrustDialog] has been confirmed - or none at all. Which of
 * these are offered depends on [webOrigin], see [WebProxyPolicy]: the hosted proxy only on the
 * official origin, the direct connection only everywhere else.
 *
 * Shared by the settings sheet and the add-account flow so the consent gate has exactly one
 * implementation; a second copy of this UI would be a second chance to get the hosted proxy
 * selected without the disclosure. Callers decide whether to show it at all (it is meaningless off
 * the web build) and supply their own heading.
 */
@Composable
fun ProxyServerSetup(
    userAppPreferencesStore: UserAppPreferencesStore,
    modifier: Modifier = Modifier,
    webOrigin: String? = getPlatform().webOrigin
) {

    val userProxyServer by userAppPreferencesStore.getUserProxyServerAsFlow().collectAsState(userAppPreferencesStore.userProxyServer)
    val hostedProxyConsentUrl by userAppPreferencesStore.getHostedProxyConsentUrlAsFlow().collectAsState(userAppPreferencesStore.hostedProxyConsentUrl)

    val hostedProxyUrl = HttpClientFactory.HOSTED_FLYIO_PROXY_URL
    val hostedProxyAvailable = WebProxyPolicy.isHostedProxyAvailable(webOrigin)
    val directConnectionAvailable = WebProxyPolicy.isDirectConnectionAvailable(webOrigin)

    // Kept around while another option is selected so switching back restores what the user had typed.
    var ownProxyServerDraft by remember { mutableStateOf(userProxyServer?.takeIf { it.trim() != hostedProxyUrl } ?: "") }
    // The selected card is UI state, read from the stored value only once: off the official origin an
    // empty value means "direct", so deriving it on every change would flip "My own proxy" back to
    // "No proxy" the moment it is clicked, before a URL is typed.
    var choice by remember {
        mutableStateOf(
            when {
                hostedProxyAvailable && userProxyServer?.trim() == hostedProxyUrl -> ProxyChoice.HOSTED
                directConnectionAvailable && userProxyServer.isNullOrBlank() -> ProxyChoice.DIRECT
                else -> ProxyChoice.OWN
            }
        )
    }
    val hostedProxySelected = choice == ProxyChoice.HOSTED
    var trustDialogVisible by remember { mutableStateOf(false) }
    var proxyPresetsExpanded by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current


    fun selectOwnProxy() {
        choice = ProxyChoice.OWN
        userAppPreferencesStore.userProxyServer = ownProxyServerDraft.ifBlank { null }
    }

    fun selectHostedProxy() {
        choice = ProxyChoice.HOSTED
        userAppPreferencesStore.userProxyServer = hostedProxyUrl
    }

    fun selectDirectProxy() {
        choice = ProxyChoice.DIRECT
        userAppPreferencesStore.userProxyServer = null
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(
            text = stringResource(Res.string.settings_proxy_server_info),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
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
            selected = choice == ProxyChoice.OWN,
            icon = Icons.Outlined.Dns,
            title = stringResource(Res.string.settings_proxy_option_own),
            info = stringResource(Res.string.settings_proxy_option_own_info),
            badge = stringResource(Res.string.settings_proxy_option_own_recommended),
            onClick = { selectOwnProxy() }
        )

        AnimatedVisibility(choice == ProxyChoice.OWN) {
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
                        // Off the official origin an empty field is saved as "no proxy", i.e. a direct
                        // connection - say so rather than let the selected card suggest a proxy is used.
                        AnimatedVisibility(directConnectionAvailable && ownProxyServerDraft.isBlank()) {
                            Text(stringResource(Res.string.settings_proxy_own_empty_connects_directly))
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

        if (directConnectionAvailable) {
            ProxyOptionCard(
                selected = choice == ProxyChoice.DIRECT,
                icon = Icons.Outlined.Lan,
                title = stringResource(Res.string.settings_proxy_option_direct),
                info = stringResource(Res.string.settings_proxy_option_direct_info),
                badge = stringResource(Res.string.settings_proxy_option_direct_badge),
                onClick = { selectDirectProxy() }
            )
        }

        // Our proxy only accepts the official origin (PROXY_ALLOWED_ORIGINS in fly.toml), so on a
        // self-hosted copy offering it would only lead to CORS failures.
        if (hostedProxyAvailable) {
            ProxyOptionCard(
                selected = hostedProxySelected,
                icon = Icons.Outlined.Cloud,
                title = stringResource(Res.string.settings_proxy_option_hosted),
                info = stringResource(Res.string.settings_proxy_option_hosted_info),
                badge = stringResource(Res.string.settings_proxy_option_hosted_badge),
                badgeColor = MaterialTheme.colorScheme.error,
                supportingText = hostedProxyUrl,
                onClick = {
                    // Consent is per URL: an instance the user never agreed to always asks first.
                    if (hostedProxyConsentUrl == hostedProxyUrl)
                        selectHostedProxy()
                    else
                        trustDialogVisible = true
                }
            )
        }

        // While the hosted proxy is in use the disclosure stays on screen - consent is given once,
        // but the user should never have to remember what they agreed to.
        AnimatedVisibility(hostedProxySelected) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.widthIn(min = 350.dp).fillMaxWidth().padding(horizontal = 16.dp)
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

    if (trustDialogVisible) {
        ProxyTrustDialog(
            proxyUrl = hostedProxyUrl,
            initiallyAccepted = hostedProxySelected,
            onConfirm = {
                userAppPreferencesStore.hostedProxyConsentUrl = hostedProxyUrl
                selectHostedProxy()
                trustDialogVisible = false
            },
            onDismiss = { trustDialogVisible = false },
            onOpenSelfHostingInfo = { uriHandler.openUri(HttpClientFactory.PROXY_SETUP_INFO_URL) }
        )
    }
}


@Composable
private fun ProxyOptionCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    info: String,
    badge: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    supportingText: String? = null
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth()
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

@Preview
@Composable
private fun ProxyServerSetup_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            ProxyServerSetup(
                userAppPreferencesStore = UserAppPreferencesStore.getEmptyPreferenceStoreForPreview(SpectacledVariant.JOURNALS),
                webOrigin = WebProxyPolicy.OFFICIAL_ORIGINS.first()
            )
        }
    }
}

@Preview
@Composable
private fun ProxyServerSetup_SelfHosted_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            ProxyServerSetup(
                userAppPreferencesStore = UserAppPreferencesStore.getEmptyPreferenceStoreForPreview(SpectacledVariant.JOURNALS),
                webOrigin = "https://cloud.example.com"
            )
        }
    }
}
