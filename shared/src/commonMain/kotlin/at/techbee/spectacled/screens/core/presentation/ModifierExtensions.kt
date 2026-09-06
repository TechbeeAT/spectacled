package at.techbee.spectacled.screens.core.presentation

import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds a fading edge effect to a horizontally scrollable container.
 *
 * @param scrollState The scroll state of the container.
 * @param length The length of the fading edge.
 */
fun Modifier.horizontalFadingEdges(
    scrollState: ScrollState,
    length: Dp = 16.dp,
): Modifier = this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()

        val fadingEdgeLengthPx = length.toPx()

        val scrollValue = scrollState.value
        val maxValue = scrollState.maxValue

        if (maxValue == 0 || maxValue == Int.MAX_VALUE) return@drawWithContent

        // Left edge fade
        if (scrollValue > 0) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startX = 0f,
                    endX = fadingEdgeLengthPx
                ),
                blendMode = BlendMode.DstIn
            )
        }

        // Right edge fade
        if (scrollValue < maxValue) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - fadingEdgeLengthPx,
                    endX = size.width
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }
