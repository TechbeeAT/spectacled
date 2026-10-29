package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_entries_found_in_this_folder
import spectacled.shared.generated.resources.no_matching_entries_found

@Composable
fun SplashScreen(
    spectacledVariant: SpectacledVariant,
    showProgressIndicator: Boolean = false,
    size: Dp = 200.dp,
    reducedAlpha: Boolean = false,
    text: String? = null,
    modifier: Modifier = Modifier
) {

    val alpha = if(reducedAlpha) 0.25f else 1f

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Box(
            modifier = Modifier
                //.padding(48.dp)
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {

            if (showProgressIndicator)
                CircularProgressIndicator(
                    modifier = Modifier.size(size * 1.24f)
                )

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {

                Image(
                    painter = painterResource(spectacledVariant.logoDrawableResource),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * 0.08f)
                )
            }
        }

        text?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(alpha * 2)
                    .padding(bottom = 88.dp, top = 40.dp, start = 32.dp, end = 32.dp)
            )
        }
    }
}

@Preview
@Composable
fun SplashScreen_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        SplashScreen(
            spectacledVariant = SpectacledVariant.JOURNALS,
            showProgressIndicator = true,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview(uiMode = 32)   // 32 = UI_MODE_NIGHT_YES
@Composable
fun SplashScreen_no_progress_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        SplashScreen(
            spectacledVariant = SpectacledVariant.NOTES,
            showProgressIndicator = false,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview
@Composable
fun SplashScreen_no_progress_withText_noMatchingEntries_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        SplashScreen(
            spectacledVariant = SpectacledVariant.NOTES,
            showProgressIndicator = false,
            text = stringResource(Res.string.no_matching_entries_found),
            reducedAlpha = true,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview
@Composable
fun SplashScreen_no_progress_withText_noEntriesFound_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        SplashScreen(
            spectacledVariant = SpectacledVariant.NOTES,
            showProgressIndicator = false,
            text = stringResource(Res.string.no_entries_found_in_this_folder),
            reducedAlpha = true,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
    }
}

@Preview
@Composable
fun SplashScreen_no_progress_withLongText_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        SplashScreen(
            spectacledVariant = SpectacledVariant.NOTES,
            showProgressIndicator = false,
            text = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren",
            reducedAlpha = true,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
    }
}