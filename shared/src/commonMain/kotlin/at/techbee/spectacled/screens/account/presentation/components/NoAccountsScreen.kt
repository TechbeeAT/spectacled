package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.components.SplashScreen
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_account_connected_yet

/**
 * Shown on the accounts screen while no principal is connected, so the screen behind the
 * welcome bottom sheet isn't blank. The call to action is the "Add account" FAB of the screen.
 */
@Composable
fun NoAccountsScreen(
    spectacledVariant: SpectacledVariant,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // fixed, modest size so the logo also fits the narrow side pane of the landscape layout
            SplashScreen(
                spectacledVariant = spectacledVariant,
                color = MaterialTheme.colorScheme.primary,
                showProgressIndicator = false,
                size = 120.dp,
                modifier = Modifier
                    .padding(24.dp)
                    .alpha(0.25f)
            )

            Text(
                text = stringResource(Res.string.no_account_connected_yet),
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(0.55f)
                    .padding(top = 8.dp, bottom = 88.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun NoAccountsScreen_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        NoAccountsScreen(spectacledVariant = SpectacledVariant.NOTES)
    }
}
