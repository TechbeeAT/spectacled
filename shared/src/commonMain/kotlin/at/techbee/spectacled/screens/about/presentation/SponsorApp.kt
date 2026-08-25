package at.techbee.spectacled.screens.about.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.Napier
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.about_more_donation_options
import spectacled.shared.generated.resources.about_sponsor
import spectacled.shared.generated.resources.about_sponsor_info
import spectacled.shared.generated.resources.paypal
import spectacled.shared.generated.resources.thank_you

@Composable
fun SponsorApp(
    spectacledVariant: SpectacledVariant
) {

    val uriHandler = LocalUriHandler.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.about_sponsor),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
        )

        Text(
            text = stringResource(Res.string.about_sponsor_info),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )

        Button(
            onClick = {
                try {
                    uriHandler.openUri("https://www.paypal.com/donate/?hosted_button_id=BKR9ZW3DNNQHS")
                } catch (e: Exception) {
                    Napier.w(e.stackTraceToString())
                }
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.paypal))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
            }
        }

        OutlinedButton(
            onClick = {
                try {
                    uriHandler.openUri("https://spectacled.techbee.at/contribute/donate/")
                } catch (e: Exception) {
                    Napier.w(e.stackTraceToString())
                }
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(Res.string.about_more_donation_options))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null)
            }
        }

        Text(
            text = stringResource(Res.string.thank_you),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 24.dp)
        )
        Icon(
            imageVector = Icons.Outlined.VolunteerActivism,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
    }
}

@Preview
@Composable
fun SponsorApp_Preview(
) {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            SponsorApp(spectacledVariant = SpectacledVariant.JOURNALS)
        }
    }
}