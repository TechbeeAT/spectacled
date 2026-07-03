package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.theme.AppTheme
import com.materialkolor.dynamicColorScheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.back
import spectacled.shared.generated.resources.ic_pin
import spectacled.shared.generated.resources.ic_unpin
import spectacled.shared.generated.resources.pin
import spectacled.shared.generated.resources.read_only
import spectacled.shared.generated.resources.unpin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsTopBar(
    onAction: (DetailsAction) -> Unit,
    canWriteContent: Boolean,
    isLoading: Boolean,
    isPinned: Boolean,
    spectacledVariant: SpectacledVariant = koinInject<SpectacledVariant>(),
    modifier: Modifier = Modifier
) {

    TopAppBar(
        navigationIcon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onAction(DetailsAction.OnNavigateUp(true)) },
                    enabled = !isLoading
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = stringResource(Res.string.back)
                        )
                        Text(text = stringResource(Res.string.back))
                    }
                }
                AnimatedVisibility(isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                }
            }
        },
        actions = {
            if (!canWriteContent) {
                TextButton(
                    onClick = {},
                    enabled = false
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditOff,
                        contentDescription = stringResource(Res.string.read_only)
                    )
                }
            } else {

                when (spectacledVariant) {
                    SpectacledVariant.JOURNALS -> {}
                    SpectacledVariant.NOTES -> {
                        TextButton(
                            onClick = { onAction(DetailsAction.OnPin(!isPinned)) }
                        ) {
                            if (isPinned)
                                Icon(
                                    painter = painterResource(Res.drawable.ic_pin),
                                    contentDescription = stringResource(Res.string.unpin)
                                )
                            else
                                Icon(
                                    painter = painterResource(Res.drawable.ic_unpin),
                                    contentDescription = stringResource(Res.string.pin)
                                )
                        }
                    }

                    SpectacledVariant.TASKS -> {}
                }
/*
                var dropdownExpanded by remember { mutableStateOf(false) }

                TextButton(onClick = { dropdownExpanded = true }) {

                    Icon(Icons.Outlined.MoreVert, null)

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("AI Extract (Claude; experimental)") },
                            onClick = {
                                dropdownExpanded = false
                                onAction(DetailsAction.OnProcessWithAI)
                            }
                        )
                    }
                }

 */
            }
        },
        modifier = modifier,
        title = {},
    )

}


@Preview
@Composable
private fun DetailsTopBar_Journals_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        DetailsTopBar(
            canWriteContent = true,
            isLoading = false,
            isPinned = false,
            onAction = {},
            spectacledVariant = SpectacledVariant.JOURNALS
        )
    }
}


@Preview
@Composable
private fun DetailsTopBar_Notes_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.NOTES) {

        DetailsTopBar(
            canWriteContent = true,
            isLoading = false,
            isPinned = false,
            onAction = {},
            spectacledVariant = SpectacledVariant.NOTES
        )
    }
}

@Preview
@Composable
private fun DetailsTopBar_readonly_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {

        DetailsTopBar(
            canWriteContent = false,
            isLoading = true,
            isPinned = true,
            onAction = {},
            spectacledVariant = SpectacledVariant.TASKS
        )
    }
}

@Preview
@Composable
private fun DetailsTopBar_TASKS_with_color_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {

        MaterialTheme(dynamicColorScheme(Color.Yellow, false)) {

            DetailsTopBar(
                canWriteContent = false,
                isLoading = true,
                isPinned = true,
                onAction = {},
                spectacledVariant = SpectacledVariant.TASKS
            )
        }
    }
}
