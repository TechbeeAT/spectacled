package at.techbee.spectacled.screenshots

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.PlatformWindowInsets
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The time every status bar shows, as in Apple's and Google's own marketing screenshots. */
private const val STATUS_BAR_TIME = "9:41"

/** The system bar insets of [device], in pixels, for LocalPlatformWindowInsets. */
@OptIn(InternalComposeUiApi::class)
internal fun systemBarInsets(device: Device): PlatformWindowInsets {
    val top = (device.topInsetDp * device.density).toInt()
    val bottom = (device.bottomInsetDp * device.density).toInt()
    return object : PlatformWindowInsets {
        override val statusBars: PlatformInsets = PlatformInsets(top = top)
        override val navigationBars: PlatformInsets = PlatformInsets(bottom = bottom)
        override val systemBars: PlatformInsets = PlatformInsets(top = top, bottom = bottom)
    }
}

/**
 * The status bar and the home indicator (iOS) or gesture handle (Android), drawn on a transparent
 * scene that is laid over the rendered app, so they stay on top of sheets and scrims.
 */
@Composable
internal fun SystemBarsOverlay(device: Device, dark: Boolean) {
    if (device.systemBars == SystemBarsStyle.NONE)
        return
    val color = if (dark) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(device.topInsetDp.dp)
                .padding(horizontal = if (device.systemBars == SystemBarsStyle.IOS && !device.isTablet) 36.dp else 20.dp)
        ) {
            when (device.systemBars) {
                SystemBarsStyle.IOS -> IosStatusBar(color)
                SystemBarsStyle.ANDROID -> AndroidStatusBar(color)
                SystemBarsStyle.NONE -> Unit
            }
        }

        // iOS home indicator, Android gesture navigation handle
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (device.systemBars == SystemBarsStyle.IOS) 8.dp else 10.dp)
                .size(
                    width = if (device.systemBars == SystemBarsStyle.IOS) (if (device.isTablet) 320.dp else 140.dp) else 108.dp,
                    height = if (device.systemBars == SystemBarsStyle.IOS) 5.dp else 4.dp
                )
                .background(color, RoundedCornerShape(50))
        )
    }
}

@Composable
private fun RowScope.IosStatusBar(color: Color) {
    Text(
        text = STATUS_BAR_TIME,
        style = TextStyle(color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif),
        modifier = Modifier.weight(1f)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        // Cellular signal: four rising bars
        Canvas(modifier = Modifier.size(18.dp, 12.dp)) {
            val barWidth = size.width / 5.5f
            val gap = (size.width - 4 * barWidth) / 3
            listOf(0.35f, 0.55f, 0.78f, 1f).forEachIndexed { index, fraction ->
                val height = size.height * fraction
                drawRoundRect(
                    color = color,
                    topLeft = Offset(index * (barWidth + gap), size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = CornerRadius(barWidth / 3)
                )
            }
        }
        // Wi-Fi: a dot and two arcs fanning out from the bottom centre
        Canvas(modifier = Modifier.size(16.dp, 12.dp)) {
            val stroke = size.height / 5.5f
            val center = Offset(size.width / 2, size.height)
            drawArc(
                color = color,
                startAngle = 225f, sweepAngle = 90f, useCenter = true,
                topLeft = center - Offset(size.height * 0.38f, size.height * 0.38f),
                size = Size(size.height * 0.76f, size.height * 0.76f)
            )
            listOf(0.68f, 0.98f).forEach { radiusFraction ->
                val radius = size.height * radiusFraction - stroke / 2
                drawArc(
                    color = color,
                    startAngle = 225f, sweepAngle = 90f, useCenter = false,
                    topLeft = center - Offset(radius, radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        // Battery: outline, full charge and the terminal cap
        Canvas(modifier = Modifier.size(27.dp, 13.dp)) {
            val capWidth = size.width * 0.06f
            val bodyWidth = size.width - capWidth - 1.dp.toPx()
            val outline = 1.dp.toPx()
            drawRoundRect(
                color = color.copy(alpha = 0.4f),
                size = Size(bodyWidth, size.height),
                cornerRadius = CornerRadius(size.height * 0.3f),
                style = Stroke(width = outline)
            )
            val inset = outline * 2
            drawRoundRect(
                color = color,
                topLeft = Offset(inset, inset),
                size = Size(bodyWidth - 2 * inset, size.height - 2 * inset),
                cornerRadius = CornerRadius(size.height * 0.2f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.4f),
                topLeft = Offset(size.width - capWidth, size.height * 0.33f),
                size = Size(capWidth, size.height * 0.34f),
                cornerRadius = CornerRadius(capWidth / 2)
            )
        }
    }
}

@Composable
private fun RowScope.AndroidStatusBar(color: Color) {
    Text(
        text = STATUS_BAR_TIME,
        style = TextStyle(color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif),
        modifier = Modifier.weight(1f)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(Icons.Filled.Wifi, Icons.Filled.SignalCellular4Bar, Icons.Filled.BatteryFull).forEach { icon ->
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        }
    }
}
