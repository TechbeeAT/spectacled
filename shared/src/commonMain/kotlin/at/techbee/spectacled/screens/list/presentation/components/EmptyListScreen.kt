package at.techbee.spectacled.screens.list.presentation.components

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.presentation.components.SplashScreen
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_entries_found_in_this_folder
import spectacled.shared.generated.resources.no_matching_entries_found

@Composable
fun EmptyListScreen(
    isEmptyFolder: Boolean,
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

            SplashScreen(
                spectacledVariant = spectacledVariant,
                color = MaterialTheme.colorScheme.primary,
                showProgressIndicator = false,
                modifier = Modifier
                    .weight(1f)
                    .padding(48.dp)
                    .alpha(0.25f)
            )

            Text(
                text = stringResource(
                    if (isEmptyFolder)
                        Res.string.no_entries_found_in_this_folder
                    else
                        Res.string.no_matching_entries_found
                ),
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .alpha(0.55f)
                    .padding(top = 8.dp, bottom = 88.dp, start = 8.dp, end = 8.dp)
            )
        }
    }
}

@Preview
@Composable
fun EmptyListScreen_empty_search_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        EmptyListScreen(
            spectacledVariant = SpectacledVariant.JOURNALS,
            isEmptyFolder = false
        )
    }
}

@Preview
@Composable
fun EmptyListScreen_empty_list_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        EmptyListScreen(
            spectacledVariant = SpectacledVariant.NOTES,
            isEmptyFolder = true
        )
    }
}


