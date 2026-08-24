package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.screens.details.presentation.DetailsSheetOrDialog
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.copied_to_clipboard
import spectacled.shared.generated.resources.copy_to_clipboard
import spectacled.shared.generated.resources.create_copy
import spectacled.shared.generated.resources.created
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.last_modified
import spectacled.shared.generated.resources.label_value
import spectacled.shared.generated.resources.move
import spectacled.shared.generated.resources.send_as_email
import spectacled.shared.generated.resources.share


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsMoreBottomSheet(
    onAction: (DetailsAction) -> Unit,
    icalEntry: IcalEntry,
    claudeUserApiKeyProvided: Boolean,
    canWriteContent: Boolean
) {

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val shareText = icalEntry.getAnnotatedStringForShare()


    BottomSheetWithMenu(
        onDismiss = { onAction(DetailsAction.OnShowSheetOrDialog(null)) },
        menuActionRight = {
            TextButton(
                onClick = { onAction(DetailsAction.OnShowSheetOrDialog(null)) },
            ) {
                Text(stringResource(Res.string.done))
            }
        },
    ) {

        val copiedToClipboardText = stringResource(Res.string.copied_to_clipboard)
        //val copyCreated = stringResource(Res.string.copy_created)

        Column {

            DropdownMenuItem(
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.DriveFileMove, stringResource(Res.string.move)) },
                text = { Text(text = stringResource(Res.string.move)) },
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.MOVE))
                },
                colors = MenuDefaults.itemColors().copy(
                    textColor = MaterialTheme.colorScheme.primary,
                    leadingIconColor = MaterialTheme.colorScheme.primary
                ),
                enabled = canWriteContent
            )

            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.Delete, stringResource(Res.string.delete)) },
                text = { Text(text = stringResource(Res.string.delete)) },
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.DELETE))
                },
                colors = MenuDefaults.itemColors().copy(
                    textColor = MaterialTheme.colorScheme.primary,
                    leadingIconColor = MaterialTheme.colorScheme.primary
                ),
                enabled = canWriteContent
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        when (getPlatform().platform) {
                            Platforms.ANDROID -> Icons.Outlined.Share
                            Platforms.IOS -> Icons.Outlined.IosShare
                            Platforms.DESKTOP, Platforms.WASM -> Icons.Outlined.Email
                        },
                        when (getPlatform().platform) {
                            Platforms.ANDROID, Platforms.IOS -> stringResource(Res.string.share)
                            Platforms.DESKTOP, Platforms.WASM -> stringResource(Res.string.send_as_email)
                        }
                    )
                },
                text = {
                    Text(
                        text = when (getPlatform().platform) {
                            Platforms.ANDROID, Platforms.IOS -> stringResource(Res.string.share)
                            Platforms.DESKTOP, Platforms.WASM -> stringResource(Res.string.send_as_email)
                        }
                    )
                },
                onClick = { onAction(DetailsAction.OnShare) },
                colors = MenuDefaults.itemColors().copy(
                    textColor = MaterialTheme.colorScheme.primary,
                    leadingIconColor = MaterialTheme.colorScheme.primary
                )
            )

            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.FileCopy, stringResource(Res.string.create_copy)) },
                text = { Text(text = stringResource(Res.string.create_copy)) },
                onClick = {
                    onAction(DetailsAction.OnShowSheetOrDialog(null))
                    onAction(DetailsAction.OnCreateCopy)
                },
                colors = MenuDefaults.itemColors().copy(
                    textColor = MaterialTheme.colorScheme.primary,
                    leadingIconColor = MaterialTheme.colorScheme.primary
                ),
                enabled = canWriteContent
            )

            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Outlined.ContentPaste, stringResource(Res.string.copy_to_clipboard)) },
                text = { Text(text = stringResource(Res.string.copy_to_clipboard)) },
                onClick = {
                    scope.launch {
                        onAction(DetailsAction.OnShowSheetOrDialog(null))
                        clipboard.setText(shareText)
                    }
                    onAction(DetailsAction.OnUpdateSnackbar(copiedToClipboardText))
                },
                colors = MenuDefaults.itemColors().copy(
                    textColor = MaterialTheme.colorScheme.primary,
                    leadingIconColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        Res.string.label_value,
                        stringResource(Res.string.created),
                        icalEntry.created.formatLocalized(IcsDateTimeFormat.DATE_TIME)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
                Text(
                    text = stringResource(
                        Res.string.label_value,
                        stringResource(Res.string.last_modified),
                        (icalEntry.lastModified ?: icalEntry.created).formatLocalized(IcsDateTimeFormat.DATE_TIME)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.5f)
                )
                /*
            Text(
                text = "Sync status: ${icalEntry.syncState.name}",
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.5f)
            )
             */
            }
        }
    }
}


@Preview
@Composable
fun DetailsMoreBottomSheetPreview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DetailsMoreBottomSheet(
                onAction = {},
                icalEntry = IcalEntry.getSampleIcalEntry(),
                canWriteContent = true,
                claudeUserApiKeyProvided = true
            )
        }
    }
}

@Preview
@Composable
fun DetailsMoreBottomSheet_ReadOnly_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DetailsMoreBottomSheet(
                onAction = {},
                icalEntry = IcalEntry.getSampleIcalEntry(),
                canWriteContent = false,
                claudeUserApiKeyProvided = false
            )
        }
    }
}