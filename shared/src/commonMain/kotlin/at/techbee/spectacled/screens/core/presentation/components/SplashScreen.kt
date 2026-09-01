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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.painterResource

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
                modifier = Modifier
                    .alpha(alpha * 2)
                    .padding(bottom = 88.dp, top = 40.dp)
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

@Preview
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
fun SplashScreen_no_progress_withText_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {
        SplashScreen(
            spectacledVariant = SpectacledVariant.NOTES,
            showProgressIndicator = false,
            text = "This is a text",
            reducedAlpha = true,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
    }
}