package at.techbee.spectacled.screens.note.presentation.notelist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.presentation.SpecialRoundedCard
import at.techbee.spectacled.screens.note.domain.Note
import at.techbee.spectacled.screens.note.domain.SyncState
import at.techbee.spectacled.screens.note.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.categories
import spectacled.shared.generated.resources.no_summary_description
import spectacled.shared.generated.resources.sync_conflict_detected
import org.jetbrains.compose.resources.stringResource
import kotlin.time.ExperimentalTime

@Composable
fun NoteListItem(
    note: Note,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    dragHandle: @Composable (() -> Unit) = { },
    modifier: Modifier = Modifier
) {

    val hapticFeedback = LocalHapticFeedback.current

    SpecialRoundedCard(
        isFirst = isFirst,
        isLast = isLast,
        isSelected = isSelected,
        colors = CardDefaults.cardColors(
            containerColor = note.color ?: Color.Unspecified,
            contentColor = note.color?.let { getContentColorForColoredSurfaces(it) } ?: contentColorFor(Color.Unspecified)
        ),
        interactionSource = interactionSource,
        onClick = {},
        modifier = modifier
    ) {

        Box(
            modifier = Modifier
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
                .padding(8.dp)
                .fillMaxWidth()
                //.fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // workaround to ensure minimum height while having a proper ripple effect on click but also to have the same height as when the drag handle is visible
                Spacer(modifier = Modifier.width(0.dp).height(48.dp))
                dragHandle()

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    if (note.summary?.isBlank() == false)
                        Text(
                            text = MarkdownVisualTransformation(LocalContentColor.current).formatAnnotatedString(note.summary),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )


                    if (note.description?.isBlank() == false)
                        Text(
                            text = MarkdownVisualTransformation(LocalContentColor.current).formatAnnotatedString(note.description),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )


                    if (note.summary.isNullOrBlank() && note.description.isNullOrBlank())
                        Text(
                            text = stringResource(Res.string.no_summary_description),
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth()
                        )


                    AnimatedVisibility (note.categories.isNotEmpty() || note.syncState.isConflictState()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if(note.categories.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = stringResource(Res.string.categories),
                                    modifier = Modifier.size(12.dp)
                                )

                                Text(
                                    text = note.categories.joinToString(separator = ","),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            AnimatedVisibility(note.syncState.isConflictState()) {
                                Icon(
                                    imageVector = Icons.Outlined.SyncProblem,
                                    contentDescription = stringResource(Res.string.sync_conflict_detected),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun NoteListItem_first_Preview() {
    NoteListItem(
        note = Note.getSampleNote(),
        isFirst = true,
        isLast = false,
        isSelected = false,
        onClick = {},
        onLongClick = {}
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun NoteListItem_last_Preview_with_color() {
    NoteListItem(
        note = Note.getSampleNote().copy(color = Color.Blue),
        isFirst = false,
        isLast = true,
        isSelected = true,
        onClick = {},
        onLongClick = {}
    )
}


@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun NoteListItem_first_and_last_Preview_with_color() {
    NoteListItem(
        note = Note.getSampleNote(),
        isFirst = true,
        isLast = true,
        isSelected = false,
        onClick = {},
        onLongClick = {}
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun NoteListItem_middle_Preview_with_color() {
    NoteListItem(
        note = Note.getSampleNote().copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED),
        isFirst = false,
        isLast = false,
        isSelected = false,
        onClick = {},
        onLongClick = {}
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun NoteListItem_middle_Preview_no_summary_and_description() {
    NoteListItem(
        note = Note.getSampleNote().copy(summary = null, description = null),
        isFirst = false,
        isLast = false,
        isSelected = true,
        onClick = {},
        onLongClick = {},
        dragHandle = {
            IconButton(
                onClick = {}
            ) {
                Icon(Icons.Outlined.DragIndicator, null)
            }
        }
    )
}





