package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.core.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.screens.core.presentation.components.SpecialRoundedCard
import at.techbee.spectacled.theme.getColorSchemeForSeedColor
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.no_summary_description
import spectacled.shared.generated.resources.sync_conflict_detected
import spectacled.shared.generated.resources.time
import kotlin.time.ExperimentalTime

@Composable
fun ListItem(
    icalEntry: IcalEntry,
    isSelected: Boolean,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFilterCategory: (category: String) -> Unit,
    isFirst: Boolean = true,
    isLast: Boolean = true,
    overrideTopRoundedCornerSize: Dp? = null,
    overrideBottomRoundedCornerSize: Dp? = null,
    showDayBlock: Boolean = false,
    dragHandle: @Composable (() -> Unit) = { },
    modifier: Modifier = Modifier
) {

    val hapticFeedback = LocalHapticFeedback.current

    MaterialTheme(colorScheme = getColorSchemeForSeedColor(icalEntry.color)) {

        Row(modifier = modifier) {
            if(icalEntry.isJournal()) {
                if (icalEntry.dtStart != null && showDayBlock)
                    DayBlock(
                        icsDateTime = icalEntry.dtStart,
                        modifier = Modifier.padding(8.dp).width(28.dp)
                    )
                else
                    Spacer(modifier = Modifier.padding(8.dp).width(28.dp))
            }


            SpecialRoundedCard(
                isFirst = isFirst,
                isLast = isLast,
                isSelected = isSelected,
                interactionSource = interactionSource,
                onClick = {},
                overrideTopRoundedCornerSize = overrideTopRoundedCornerSize,
                overrideBottomRoundedCornerSize = overrideBottomRoundedCornerSize
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
                            verticalArrangement = Arrangement.spacedBy(0.dp)
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

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            ) {

                                icalEntry.dtStart?.let {
                                    if (it.isDateOnly)
                                        return@let

                                    MetaInfoCard(
                                        icon = Icons.Outlined.Schedule,
                                        iconContentDescription = stringResource(Res.string.time),
                                        text = it.formatLocalized(IcsDateTimeFormat.TIME)
                                    )
                                }

                                if (icalEntry.status in listOf(Status.CANCELLED, Status.DRAFT)) {
                                    MetaInfoCard(
                                        icon = null,
                                        iconContentDescription = null,
                                        text = stringResource(icalEntry.status?.stringRes ?: Status.FINAL.stringRes)
                                    )
                                }

                                icalEntry.categoriesWithoutPinned.forEach { category ->
                                    MetaInfoCard(
                                        icon = Icons.AutoMirrored.Outlined.Label,
                                        iconContentDescription = stringResource(Res.string.category),
                                        text = category,
                                        onClick = { onFilterCategory(category) }
                                    )
                                }

                                AnimatedVisibility(icalEntry.syncState.isConflictState()) {
                                    MetaInfoCard(
                                        icon = Icons.Outlined.SyncProblem,
                                        iconContentDescription = stringResource(Res.string.sync_conflict_detected),
                                        text = null
                                    )
                                }
                            }

                            icalEntry.attachments.filter { it.isSVG() || it.isImage() }.forEach {
                                AttachmentPreview(
                                    attachment = it,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(8.dp)
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
private fun ListItem_first_Preview() {
    ListItem(
        icalEntry = IcalEntry.getSampleIcalEntry(),
        isFirst = true,
        isLast = false,
        isSelected = false,
        onClick = {},
        onLongClick = {},
        onFilterCategory = {}
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun ListItem_last_Preview_with_color() {
    ListItem(
        icalEntry = IcalEntry.getSampleIcalEntry().copy(color = Color.Green),
        isFirst = false,
        isLast = true,
        isSelected = true,
        onClick = {},
        onLongClick = {},
        onFilterCategory = {}
    )
}


@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun ListItem_first_and_last_Preview_with_color() {
    ListItem(
        icalEntry = IcalEntry.getSampleIcalEntry(),
        isFirst = true,
        isLast = true,
        isSelected = false,
        onClick = {},
        onLongClick = {},
        onFilterCategory = {}
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun ListItem_middle_Preview_with_color() {
    ListItem(
        icalEntry = IcalEntry.getSampleIcalEntry().copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED),
        isFirst = false,
        isLast = false,
        isSelected = false,
        onClick = {},
        onLongClick = {},
        onFilterCategory = {}
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun ListItem_middle_Preview_no_summary_and_description() {
    ListItem(
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
        },
        onFilterCategory = {}
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun ListItem_withTime_Preview() {
    ListItem(
        icalEntry = IcalEntry.getSampleJournal(),
        isFirst = true,
        isLast = false,
        isSelected = false,
        onClick = {},
        onLongClick = {},
        onFilterCategory = {},
        showDayBlock = true
    )
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun ListItem_Task_with_dtstart_Preview() {
    ListItem(
        icalEntry = IcalEntry.getSampleTask(),
        isFirst = true,
        isLast = false,
        isSelected = false,
        onClick = {},
        onLongClick = {},
        onFilterCategory = {},
        showDayBlock = false
    )
}







