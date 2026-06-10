package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.theme.AppTheme
import kotlin.math.sin

@Composable
fun WavyHorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = DividerDefaults.color,
    waveLength: Float = 150f,  // Distance for one complete wave cycle
    waveAmplitude: Float = 7f // Height of the wave
) {
    Canvas(modifier = modifier.fillMaxWidth().height((waveAmplitude * 2).dp)) {
        val path = Path()
        val width = size.width
        val midY = size.height / 2

        path.moveTo(0f, midY)

        // Draw the wave across the width
        var x = 0f
        while (x <= width) {
            val angle = (x / waveLength) * (2 * 3.14159f /* pi */)
            val y = midY + (sin(angle) * waveAmplitude)
            path.lineTo(x, y)
            x += 2f // Step size for smoothness
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Preview
@Composable
fun WavyHorizontalDivider_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth().height(100.dp)
        ) {
            WavyHorizontalDivider()
        }
    }
}