package at.techbee.spectacled.screens.account.presentation.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.ai_provider
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.settings_appearance

sealed class SettingsTabDestination (
    val titleResource: StringResource,
    val icon: ImageVector
) {
    data object Appearance: SettingsTabDestination(
        titleResource = Res.string.settings_appearance,
        icon = Icons.Outlined.Palette,
    )

    data object AiProvider: SettingsTabDestination(
        titleResource = Res.string.ai_provider,
        icon = Icons.Outlined.AutoAwesome,
    )

    data object More: SettingsTabDestination(
        titleResource = Res.string.more,
        icon = Icons.Outlined.SettingsEthernet,
    )
}
