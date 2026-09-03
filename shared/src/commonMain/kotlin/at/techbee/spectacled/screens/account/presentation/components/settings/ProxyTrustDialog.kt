package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.proxy_trust_confirm_button
import spectacled.shared.generated.resources.proxy_trust_confirmation
import spectacled.shared.generated.resources.proxy_trust_intro
import spectacled.shared.generated.resources.proxy_trust_point_alternatives
import spectacled.shared.generated.resources.proxy_trust_point_credentials
import spectacled.shared.generated.resources.proxy_trust_point_promise
import spectacled.shared.generated.resources.proxy_trust_self_host_button
import spectacled.shared.generated.resources.proxy_trust_title

/**
 * Informed-consent dialog shown before the hosted CORS proxy is selected in the settings.
 *
 * The hosted proxy sees the CalDAV credentials of everyone who uses it, so switching to it must be a
 * deliberate act: the confirm button stays disabled until the user ticks the acknowledgement, and the
 * self-hosting alternative is offered right next to it.
 *
 * @param proxyUrl the hosted instance the user is about to trust
 * @param initiallyAccepted pre-ticks the acknowledgement when the dialog is re-opened to review a
 *   consent that was already given
 * @param onOpenSelfHostingInfo opens the self-hosting instructions (external link)
 */
@Composable
fun ProxyTrustDialog(
    proxyUrl: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSelfHostingInfo: () -> Unit,
    initiallyAccepted: Boolean = false
) {

    var acknowledged by remember { mutableStateOf(initiallyAccepted) }

    AlertDialog(
        icon = { Icon(Icons.Outlined.Key, null) },
        title = { Text(stringResource(Res.string.proxy_trust_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(stringResource(Res.string.proxy_trust_intro, proxyUrl))
                Text(
                    text = stringResource(Res.string.proxy_trust_point_credentials),
                    color = MaterialTheme.colorScheme.error
                )
                Text(stringResource(Res.string.proxy_trust_point_promise))
                Text(stringResource(Res.string.proxy_trust_point_alternatives))

                TextButton(onClick = { onOpenSelfHostingInfo() }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.proxy_trust_self_host_button),
                            textAlign = TextAlign.Start
                        )
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { acknowledged = !acknowledged }
                ) {
                    Checkbox(
                        checked = acknowledged,
                        onCheckedChange = { acknowledged = it }
                    )
                    Text(
                        text = stringResource(Res.string.proxy_trust_confirmation),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm() },
                enabled = acknowledged
            ) {
                Text(stringResource(Res.string.proxy_trust_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}


@Preview
@Composable
private fun ProxyTrustDialog_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            ProxyTrustDialog(
                proxyUrl = HttpClientFactory.HOSTED_WEB_PROXY_URL,
                onConfirm = {},
                onDismiss = {},
                onOpenSelfHostingInfo = {}
            )
        }
    }
}
