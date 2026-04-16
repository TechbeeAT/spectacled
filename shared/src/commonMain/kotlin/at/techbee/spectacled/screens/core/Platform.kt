package at.techbee.spectacled.screens.core

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource


enum class Platforms { ANDROID, IOS, DESKTOP, WASM }

interface Platform {
    val platform: Platforms
    val name: String

    fun isIos() = platform == Platforms.IOS

    @Composable
    fun PlatformDependentImage(
        resourceXML: DrawableResource,
        resourceSVG: DrawableResource,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        colorFilter: ColorFilter? = null
    ) {
        if (this.platform == Platforms.ANDROID)
            Image(
                imageVector = vectorResource(resourceXML),
                contentDescription = contentDescription,
                colorFilter = colorFilter,
                modifier = modifier
            )
        else
            Image(
                painter = painterResource(resourceSVG),
                contentDescription = contentDescription,
                colorFilter = colorFilter,
                modifier = modifier
            )
    }
}

expect fun getPlatform(): Platform
