package at.techbee.spectacled.screens.list.presentation.datastructures

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.view_grid
import spectacled.shared.generated.resources.view_list

enum class ListLayout(
    val displayName: StringResource,
    val displayIcon: ImageVector
) {
    LIST(
        Res.string.view_list,
        Icons.AutoMirrored.Outlined.ViewList
    ),
    STAGGERED_GRID(
        Res.string.view_grid,
        Icons.Outlined.GridView
    )
}