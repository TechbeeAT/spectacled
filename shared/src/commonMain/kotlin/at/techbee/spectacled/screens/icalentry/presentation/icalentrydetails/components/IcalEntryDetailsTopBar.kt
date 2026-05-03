package at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.IcalEntryDetailsAction
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
fun IcalEntryDetailsTopBar(
    onAction: (IcalEntryDetailsAction) -> Unit,
    canWriteContent: Boolean,
    contentColor: Color,
    isLoading: Boolean,
    isPinned: Boolean,
    spectacledVariant: SpectacledVariant = koinInject<SpectacledVariant>(),
    modifier: Modifier = Modifier
) {


    //var dropdownMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onAction(IcalEntryDetailsAction.OnNavigateUp(true)) },
                    enabled = !isLoading
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = stringResource(Res.string.back),
                            tint = contentColor
                        )
                        Text(
                            text = stringResource(Res.string.back),
                            color = contentColor
                        )
                    }
                }
                AnimatedVisibility(isLoading) {
                    CircularProgressIndicator(
                        color = if(contentColor == Color.Unspecified) ProgressIndicatorDefaults.circularColor else contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        actions = {
            if(!canWriteContent) {
                TextButton(
                    onClick = {},
                    enabled = false
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditOff,
                        contentDescription = stringResource(Res.string.read_only),
                        tint = contentColor
                    )
                }
            } else if (spectacledVariant == SpectacledVariant.NOTES){
                TextButton(
                    onClick = { onAction(IcalEntryDetailsAction.OnPinIcalEntry(!isPinned)) }
                ) {
                    if(isPinned)
                        Icon(
                            painter = painterResource(Res.drawable.ic_pin),
                            contentDescription = stringResource(Res.string.unpin),
                            tint = contentColor
                        )
                    else
                        Icon(
                            painter = painterResource(Res.drawable.ic_unpin),
                            contentDescription = stringResource(Res.string.pin),
                            tint = contentColor
                        )
                }
            }
            /*
            IconButton(
                onClick = {}
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationImportant,
                    contentDescription = "Pin",
                    tint = iconTint
                )
            }

            IconButton(
                onClick = {
                    dropdownMenuExpanded = !dropdownMenuExpanded
                }
            ) {
                Icon(
                    imageVector = if(getPlatform().platform == Platforms.IOS)
                        Icons.Outlined.Pending
                    else
                        Icons.Outlined.MoreVert,
                    contentDescription = stringResource(Res.string.more),
                    tint = iconTint
                )

                DropdownMenu(
                    expanded = dropdownMenuExpanded,
                    onDismissRequest = { dropdownMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.DeleteForever, stringResource(Res.string.delete))},
                        text = { Text(text = stringResource(Res.string.delete)) },
                        onClick = { onAction(NoteDetailsAction.OnDeleteNote) },
                    )

                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.Share, stringResource(Res.string.delete))},
                        text = { Text(text = stringResource(Res.string.share)) },
                        onClick = { onAction(NoteDetailsAction.OnShareNote) },
                    )

                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.FileCopy, stringResource(Res.string.create_copy))},
                        text = { Text(text = stringResource(Res.string.create_copy)) },
                        onClick = { onAction(NoteDetailsAction.OnCopyNote) },
                    )

                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Outlined.ContentPaste, stringResource(Res.string.copy_to_clipboard))},
                        text = { Text(text = stringResource(Res.string.copy_to_clipboard)) },
                        onClick = { onAction(NoteDetailsAction.OnShareNote) },
                    )
                }
            }
        */

        },
        modifier = modifier,
        title = {},
    )

}


@Preview
@Composable
private fun IcalEntryDetailsTopBar_Preview() {
    IcalEntryDetailsTopBar(
        canWriteContent = true,
        contentColor = Color.Unspecified,
        isLoading = false,
        isPinned = false,
        onAction = {}
    )
}

@Preview
@Composable
private fun IcalEntryDetailsTopBar_readonly_Preview() {
    IcalEntryDetailsTopBar(
        canWriteContent = false,
        contentColor = Color.Unspecified,
        isLoading = true,
        isPinned = true,
        onAction = {}
    )
}
