package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.core.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.theme.getColorSchemeForSeedColor
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start
import spectacled.shared.generated.resources.no_summary_description
import spectacled.shared.generated.resources.sync_conflict_detected
import kotlin.time.ExperimentalTime

@Composable
fun TaskListItem(
    icalEntry: IcalEntry,
    isSelected: Boolean,
    allowEditing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleProgress: () -> Unit,
    onFilterCategory: (category: String) -> Unit,
    dragHandle: @Composable (() -> Unit) = { },
    modifier: Modifier = Modifier
) {

    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }

    MaterialTheme(colorScheme = getColorSchemeForSeedColor(icalEntry.color)) {

        ElevatedFilterChip(
            modifier = modifier,
            interactionSource = interactionSource,
            leadingIcon = { dragHandle() },
            trailingIcon = {
                TriStateCheckbox(
                    enabled = allowEditing && !icalEntry.syncState.isDeletedState(),
                    state = icalEntry.getProgressTriState(),
                    onClick = {
                        onToggleProgress()
                    })
            },
            label = {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(vertical = 4.dp)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                            onLongClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongClick()
                            }
                        )

                ) {

                    if (icalEntry.summary?.isBlank() == false)
                        Text(
                            text = MarkdownVisualTransformation(LocalContentColor.current).formatAnnotatedString(icalEntry.summary),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )


                    if (icalEntry.description?.isBlank() == false)
                        Text(
                            text = MarkdownVisualTransformation(LocalContentColor.current).formatAnnotatedString(icalEntry.description),
                            maxLines = 2,
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
                            MetaInfoCard(
                                icon = Icons.Outlined.CalendarToday,
                                iconContentDescription = stringResource(Res.string.date_start),
                                text = stringResource(Res.string.date_start) + " " + it.formatLocalized(if(it.isDateOnly) IcsDateTimeFormat.DATE else IcsDateTimeFormat.DATE_TIME)
                            )
                        }

                        icalEntry.due?.let {
                            MetaInfoCard(
                                icon = Icons.Outlined.CalendarToday,
                                iconContentDescription = stringResource(Res.string.date_due),
                                text = stringResource(Res.string.date_due) + " " + it.formatLocalized(if(it.isDateOnly) IcsDateTimeFormat.DATE else IcsDateTimeFormat.DATE_TIME)
                            )
                        }

                        if (icalEntry.status in listOf(Status.CANCELLED, Status.DRAFT)) {
                            MetaInfoCard(
                                icon = icalEntry.status?.vectorIcon,
                                iconContentDescription = icalEntry.status?.stringRes?.let { stringResource(it) },
                                text = stringResource(icalEntry.status?.stringRes ?: Status.FINAL.stringRes)
                            )
                        }

                        icalEntry.categories.forEach { category ->
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
                }
            },
            selected = isSelected,
            onClick = { /* Handled by internal Column */ }
        )
    }
}


@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TaskListItem_first_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        TaskListItem(
            icalEntry = IcalEntry.getSampleIcalEntry(),
            isSelected = false,
            allowEditing = true,
            onClick = {},
            onLongClick = {},
            onFilterCategory = {},
            onToggleProgress = {}
        )
    }
}


@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TaskListItem_colored_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        MaterialTheme(getColorSchemeForSeedColor(Color.Yellow)) {

            TaskListItem(
                icalEntry = IcalEntry.getSampleIcalEntry(),
                isSelected = false,
                allowEditing = true,
                onClick = {},
                onLongClick = {},
                onFilterCategory = {},
                onToggleProgress = {}
            )
        }
    }
}


@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TaskListItem_drag_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        TaskListItem(
            icalEntry = IcalEntry.getSampleIcalEntry(),
            isSelected = false,
            allowEditing = true,
            onClick = {},
            onLongClick = {},
            onFilterCategory = {},
            dragHandle = {
                IconButton(
                    onClick = {}
                ) {
                    Icon(Icons.Outlined.DragIndicator, null)
                }
            },
            onToggleProgress = {}
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview
@Composable
private fun TaskListItem_drag_short_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        TaskListItem(
            icalEntry = IcalEntry(calendarComponent = CalendarComponent.VTODO, summary = "short summary", syncState = SyncState.LOCAL_DELETED),
            isSelected = false,
            allowEditing = false,
            onClick = {},
            onLongClick = {},
            onFilterCategory = {},
            dragHandle = {
                IconButton(
                    onClick = {}
                ) {
                    Icon(Icons.Outlined.DragIndicator, null)
                }
            },
            onToggleProgress = {}
        )
    }
}

