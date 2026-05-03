package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.components

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
import androidx.compose.material.icons.outlined.Schedule
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
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.presentation.SpecialRoundedCard
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import at.techbee.spectacled.screens.icalentry.domain.SyncState
import at.techbee.spectacled.screens.icalentry.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.categories
import spectacled.shared.generated.resources.no_summary_description
import spectacled.shared.generated.resources.sync_conflict_detected
import spectacled.shared.generated.resources.time
import kotlin.time.ExperimentalTime

@Composable
fun IcalEntryListItem(
    icalEntry: IcalEntry,
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
            containerColor = icalEntry.color ?: Color.Unspecified,
            contentColor = icalEntry.color?.let { getContentColorForColoredSurfaces(it) } ?: contentColorFor(Color.Unspecified)
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

                    if (icalEntry.summary?.isBlank() == false)
                        Text(
                            text = MarkdownVisualTransformation(LocalContentColor.current).formatAnnotatedString(icalEntry.summary),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )


                    if (icalEntry.description?.isBlank() == false)
                        Text(
                            text = MarkdownVisualTransformation(LocalContentColor.current).formatAnnotatedString(icalEntry.description),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )


                    if (icalEntry.summary.isNullOrBlank() && icalEntry.description.isNullOrBlank())
                        Text(
                            text = stringResource(Res.string.no_summary_description),
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.fillMaxWidth()
                        )

                    icalEntry.dtStart?.let {
                        if(it.isDateOnly)
                            return@let

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = stringResource(Res.string.time),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = PlatformInstantFormatter(it).formatLocalizedTime(),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }


                    AnimatedVisibility (icalEntry.categories.isNotEmpty() || icalEntry.syncState.isConflictState()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if(icalEntry.categories.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Label,
                                    contentDescription = stringResource(Res.string.categories),
                                    modifier = Modifier.size(12.dp)
                                )

                                Text(
                                    text = icalEntry.categories.joinToString(separator = ","),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            AnimatedVisibility(icalEntry.syncState.isConflictState()) {
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
private fun IcalEntryListItem_first_Preview() {
    IcalEntryListItem(
        icalEntry = IcalEntry.getSampleIcalEntry(),
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
private fun IcalEntryListItem_last_Preview_with_color() {
    IcalEntryListItem(
        icalEntry = IcalEntry.getSampleIcalEntry().copy(color = Color.Blue),
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
private fun IcalEntryListItem_first_and_last_Preview_with_color() {
    IcalEntryListItem(
        icalEntry = IcalEntry.getSampleIcalEntry(),
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
private fun IcalEntryListItem_middle_Preview_with_color() {
    IcalEntryListItem(
        icalEntry = IcalEntry.getSampleIcalEntry().copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED),
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
private fun IcalEntryListItem_middle_Preview_no_summary_and_description() {
    IcalEntryListItem(
        icalEntry = IcalEntry.getSampleIcalEntry().copy(summary = null, description = null),
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

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun IcalEntryListItem_withTime_Preview() {
    IcalEntryListItem(
        icalEntry = IcalEntry.getSampleIcalEntry().copy(dtStart = IcsDateTime.now()),
        isFirst = true,
        isLast = false,
        isSelected = false,
        onClick = {},
        onLongClick = {}
    )
}





