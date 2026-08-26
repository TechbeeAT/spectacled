package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.insecure_connection_warning
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.settings_proxy_server
import spectacled.shared.generated.resources.settings_proxy_server_info


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMorePage(
    userAppPreferencesStore: UserAppPreferencesStore,
    modifier: Modifier = Modifier
) {

    var userProxyServerDropdownExpanded by remember { mutableStateOf(false) }
    val userProxyServer by userAppPreferencesStore.getUserProxyServerAsFlow().collectAsState(userAppPreferencesStore.userProxyServer)

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
                        Text(stringResource(Res.string.settings_proxy_server_info))
                    }
                },
                label = { Text(stringResource(Res.string.settings_proxy_server)) },
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
                                        Text("http://localhost:8088")
                                    }
                                },
                                onClick = {
                                    userAppPreferencesStore.userProxyServer = "http://localhost:8088"
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
