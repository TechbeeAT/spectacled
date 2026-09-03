package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.settings_proxy_server


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMorePage(
    userAppPreferencesStore: UserAppPreferencesStore,
    modifier: Modifier = Modifier
) {

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

            ProxyServerSetup(userAppPreferencesStore)
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
