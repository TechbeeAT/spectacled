package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces

@Composable
fun MetaInfoCard(
    icon: ImageVector?,
    iconContentDescription: String?,
    text: String?,
    containerColor: Color? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = containerColor ?: Color.Unspecified,
            contentColor = containerColor?.let { getContentColorForColoredSurfaces(it) } ?: contentColorFor(Color.Unspecified)
        ),
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp)
        ) {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = iconContentDescription,
                    modifier = Modifier.size(12.dp)
                )
            }

            text?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}


@Composable
@Preview
private fun MetaInfoCard_Preview() {
    MetaInfoCard(
        icon = Icons.Outlined.Schedule,
        iconContentDescription = "Time",
        text = "Time"
    )
}
