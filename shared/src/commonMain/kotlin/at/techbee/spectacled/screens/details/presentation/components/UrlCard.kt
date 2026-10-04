package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.screens.details.presentation.DetailsSheetOrDialog
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.edit
import spectacled.shared.generated.resources.open_in_browser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlCard(
    url: Url,
    allowEditing: Boolean,
    onClick: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val uriHandler = LocalUriHandler.current

    Card(
        onClick = {
            if(allowEditing)
                onClick(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.EDIT_URL))
        },
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        modifier = modifier
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(
                onClick = {},
                enabled = false
            ) {
                Icon(Icons.Outlined.Link, stringResource(Res.string.edit))
            }

            Text(
                text = url.toString(),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    try {
                        uriHandler.openUri(url.toString())
                    } catch (e: Exception) {
                        Napier.w(e.stackTraceToString())
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(Res.string.open_in_browser))
            }
        }
    }
}


@Preview
@Composable
private fun UrlCard_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        UrlCard(
            url = Url("https://spectacled.techbee.at/folder"),
            allowEditing = true,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun UrlCard_readonly_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        UrlCard(
            url = Url("https://spectacled.techbee.at/folder"),
            allowEditing = false,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

