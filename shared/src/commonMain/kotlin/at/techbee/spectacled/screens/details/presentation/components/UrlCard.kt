package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import at.techbee.spectacled.theme.AppTheme
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlCard(
    url: Url,
    onClick: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {

    val uriHandler = LocalUriHandler.current

    Card(
        onClick = {
            try {
                uriHandler.openUri(url.toString())
            } catch (e: Exception) {
                Napier.w(e.stackTraceToString())
            }
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
                    onClick(DetailsAction.OnShowEditUrlBottomSheet(true))
                }
            ) {
                Icon(Icons.Outlined.Edit, stringResource(Res.string.edit))
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
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}
