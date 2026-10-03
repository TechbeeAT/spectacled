package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PinDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.screens.details.presentation.DetailsSheetOrDialog
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationCard(
    location: String,
    allowEditing: Boolean,
    onClick: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        onClick = {
            TODO()
        },
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        modifier = modifier
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            IconButton(
                onClick = {},
                enabled = false
            ) {
                Icon(Icons.Outlined.PinDrop, null)
            }

            Text(
                text = location,
                modifier = Modifier.weight(1f)
            )

            if(allowEditing) {
                IconButton(
                    onClick = {
                        onClick(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.EDIT_LOCATION))
                    }
                ) {
                    Icon(Icons.Outlined.Edit, stringResource(Res.string.edit))
                }
            }
        }
    }
}


@Preview
@Composable
private fun LocationCard_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        LocationCard(
            location = "Am Stadtpark 123/4c, 1030 Wien",
            allowEditing = true,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun LocationCard_readonly_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        LocationCard(
            location = "Am Stadtpark 123/4c, 1030 Wien",
            allowEditing = false,
            onClick = {},
            modifier = Modifier.padding(8.dp)
        )
    }
}

