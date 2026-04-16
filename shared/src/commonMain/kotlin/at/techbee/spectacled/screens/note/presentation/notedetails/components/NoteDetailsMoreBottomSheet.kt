package at.techbee.spectacled.screens.note.presentation.notedetails.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.ShareContent
import at.techbee.spectacled.screens.core.ShareManager
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import at.techbee.spectacled.screens.note.domain.Note
import at.techbee.spectacled.screens.note.presentation.notedetails.NoteDetailsAction
import at.techbee.spectacled.theme.AppTheme
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.copied_to_clipboard
import spectacled.shared.generated.resources.copy_to_clipboard
import spectacled.shared.generated.resources.create_copy
import spectacled.shared.generated.resources.created
import spectacled.shared.generated.resources.delete
import spectacled.shared.generated.resources.last_modified
import spectacled.shared.generated.resources.send_as_email
import spectacled.shared.generated.resources.share
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailsMoreBottomSheet(
    onAction: (NoteDetailsAction) -> Unit,
    note: Note,
    canWriteContent: Boolean,
    shareManager: ShareManager = koinInject<PlatformShareManager>()
) {

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val shareText = note.getAnnotatedStringForShare()


    BottomSheetWithMenu(
        onDismiss = { onAction(NoteDetailsAction.OnShowMoreBottomSheet(false)) },
    ) {

        val copiedToClipboardText = stringResource(Res.string.copied_to_clipboard)
        //val copyCreated = stringResource(Res.string.copy_created)

        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.DeleteForever, stringResource(Res.string.delete)) },
            text = { Text(text = stringResource(Res.string.delete)) },
            onClick = {
                onAction(NoteDetailsAction.OnShowDeleteNoteDialog(true))
                onAction(NoteDetailsAction.OnShowMoreBottomSheet(false))
            },
            colors = MenuDefaults.itemColors().copy(
                textColor = MaterialTheme.colorScheme.primary,
                leadingIconColor = MaterialTheme.colorScheme.primary
            ),
            enabled = canWriteContent
        )

        // TODO: Skipping this button for iOS for now as it fails, reason is not so clear
        if(getPlatform().platform != Platforms.IOS) {
            DropdownMenuItem(
                leadingIcon = {
                    if (getPlatform().platform == Platforms.ANDROID || getPlatform().platform == Platforms.IOS)
                        Icon(Icons.Outlined.Share, stringResource(Res.string.share))
                    else
                        Icon(Icons.Outlined.Email, stringResource(Res.string.send_as_email))
                },
                text = {
                    Text(
                        text = stringResource(
                            if (getPlatform().platform == Platforms.ANDROID || getPlatform().platform == Platforms.IOS)
                                Res.string.share
                            else
                                Res.string.send_as_email
                        )
                    )
                },
                onClick = {
                    onAction(NoteDetailsAction.OnShowMoreBottomSheet(false))
                    shareManager.share(
                        ShareContent(
                            subject = note.summary ?: "",
                            body = shareText.text
                        )
                    )
                },
                colors = MenuDefaults.itemColors().copy(
                    textColor = MaterialTheme.colorScheme.primary,
                    leadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.FileCopy, stringResource(Res.string.create_copy)) },
            text = { Text(text = stringResource(Res.string.create_copy)) },
            onClick = {
                onAction(NoteDetailsAction.OnShowMoreBottomSheet(false))
                onAction(NoteDetailsAction.OnCreateCopy)
                //TODO: Make sure also new notes can be copied (id should not be 0L!)
                //TODO: Make sure note is saved before navigating
                //TODO: Navigate!
                //onNavigate(Route.AddNote(detailsState.note.calendarId, detailsState.note.id))
                //noteDetailsViewModel.onAction(NoteDetailsAction.OnUpdateSnackbar(copyCreated))
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
                    onAction(NoteDetailsAction.OnShowMoreBottomSheet(false))
                    clipboard.setText(shareText)
                }
                onAction(NoteDetailsAction.OnUpdateSnackbar(copiedToClipboardText))
            },
            colors = MenuDefaults.itemColors().copy(
                textColor = MaterialTheme.colorScheme.primary,
                leadingIconColor = MaterialTheme.colorScheme.primary
            )
        )

        Text(
            text = "${stringResource(Res.string.created)}: ${PlatformInstantFormatter(note.created).formatLocalizedDateTime()}",
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 24.dp, bottom = 0.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
        )
        note.lastModified?.let {
            Text(
                text = "${stringResource(Res.string.last_modified)}: ${PlatformInstantFormatter(it).formatLocalizedDateTime()}",
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 0.dp, bottom = 8.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
            )
        }
    }
}


@Preview
@Composable
fun NoteDetailsMoreBottomSheetPreview() {
    AppTheme {
        NoteDetailsMoreBottomSheet(
            onAction = {},
            note = Note.getSampleNote(),
            canWriteContent = true,
            shareManager = object: ShareManager {
                override fun share(content: ShareContent) {
                    println("Sharing: $content")
                }
            }
        )
    }
}

@Preview
@Composable
fun NoteDetailsMoreBottomSheet_ReadOnly_Preview() {
    AppTheme {
        NoteDetailsMoreBottomSheet(
            onAction = {},
            note = Note.getSampleNote(),
            canWriteContent = false,
            shareManager = object: ShareManager {
                override fun share(content: ShareContent) {
                    println("Sharing: $content")
                }
            }
        )
    }
}