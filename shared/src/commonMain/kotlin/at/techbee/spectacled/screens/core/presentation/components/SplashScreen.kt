package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun SplashScreen(
    spectacledVariant: SpectacledVariant,
    showProgressIndicator: Boolean = false,
    color: Color? = null,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        if(showProgressIndicator)
            CircularProgressIndicator(
                modifier = Modifier.size(248.dp)
            )

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(color ?: spectacledVariant.themeSeedColor),
            contentAlignment = Alignment.Center
        ) {

            Image(
                painter = painterResource(spectacledVariant.logoDrawableResource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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