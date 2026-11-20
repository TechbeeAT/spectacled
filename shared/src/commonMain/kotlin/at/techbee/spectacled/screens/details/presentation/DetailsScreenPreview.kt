package at.techbee.spectacled.screens.details.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.details.presentation.components.AlarmCard
import at.techbee.spectacled.screens.details.presentation.components.AttachmentCard
import at.techbee.spectacled.screens.details.presentation.components.LocationCard
import at.techbee.spectacled.screens.details.presentation.components.UrlCard
import at.techbee.spectacled.screens.list.presentation.components.MetaInfoCard
import at.techbee.spectacled.screens.list.presentation.components.TaskListItem
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.theme.getColorSchemeForSeedColor
import com.mikepenz.markdown.m3.Markdown
import io.ktor.http.Url
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.date
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start


@Composable
fun DetailsScreenPreview(
    state: DetailsState,
    modifier: Modifier = Modifier,
    onAction: (DetailsAction) -> Unit
) {

    MaterialTheme(colorScheme = getColorSchemeForSeedColor(state.icalEntry.color ?: state.calendar?.color)) {

        val uriHandler = LocalUriHandler.current

        Column(
            modifier = modifier.verticalScroll(rememberScrollState())
        ) {

            state.icalEntry.dtStart?.let {
                Text(
                    text = stringResource(if(state.icalEntry.isJournal()) Res.string.date else Res.string.date_start),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(it.formatLocalized(if(it.isDateOnly) IcsDateTimeFormat.DATE else IcsDateTimeFormat.DATE_TIME))
                Spacer(modifier = Modifier.height(8.dp))
            }

            state.icalEntry.due?.let {
                Text(
                    text = stringResource(Res.string.date_due),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(it.formatLocalized(if(it.isDateOnly) IcsDateTimeFormat.DATE else IcsDateTimeFormat.DATE_TIME))
                Spacer(modifier = Modifier.height(8.dp))
            }

            /*
            if (state.icalEntry.isTask() || state.icalEntry.isJournal()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.icalEntry.isJournal()) {
                        DateTimeCard(
                            icsDateTime = state.icalEntry.dtStart,
                            enabled = false,
                            allowNoDate = false,
                            initializeWithDateOnly = true,
                            suggestedTimezones = emptyList(),
                            onIcsDateTimeUpdated = { /* no-op */ }
                        )

                    } else if (state.icalEntry.isTask()) {
                        DateTimeCard(
                            icsDateTime = state.icalEntry.dtStart,
                            enabled = false,
                            allowNoDate = true,
                            initializeWithDateOnly = state.icalEntry.due?.isDateOnly != false,  // only when due is NOT date only we initialize with time
                            suggestedTimezones = emptyList(),
                            onIcsDateTimeUpdated = { /* no-op */ },
                            headerText = stringResource(Res.string.date_start)
                        )

                        DateTimeCard(
                            icsDateTime = state.icalEntry.due,
                            enabled = false,
                            allowNoDate = true,
                            initializeWithDateOnly = state.icalEntry.dtStart?.isDateOnly != false,  // only when dtStart is NOT date only we initialize with time
                            suggestedTimezones = emptyList(),
                            onIcsDateTimeUpdated = { /* no-op */ },
                            headerText = stringResource(Res.string.date_due)
                        )
                    }

                    //HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp))
                    WavyHorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
            }

             */

            AnimatedVisibility(
                state.icalEntry.categoriesWithoutPinned.isNotEmpty() || state.icalEntry.status in listOf(
                    Status.DRAFT,
                    Status.CANCELLED
                )
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {

                    AnimatedVisibility(state.icalEntry.status in listOf(Status.DRAFT, Status.CANCELLED)) {
                        MetaInfoCard(
                            icon = null,
                            iconContentDescription = null,
                            text = stringResource(state.icalEntry.status?.stringRes ?: Status.DRAFT.stringRes)
                        )
                    }

                    state.icalEntry.categoriesWithoutPinned.sorted().forEach { category ->

                        MetaInfoCard(
                            icon = Icons.AutoMirrored.Outlined.Label,
                            iconContentDescription = stringResource(Res.string.category),
                            text = category,
                            onClick = { /* no-op */ }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
            ) {

                state.icalEntry.summary?.let {
                    Markdown(it, modifier = Modifier.weight(1f))
                }

                if (state.icalEntry.isTask()) {
                    val entryTriState = state.icalEntry.getProgressTriState()
                    TriStateCheckbox(
                        state = entryTriState,
                        enabled = false,
                        onClick = { /* no-op */ }
                    )
                }
            }

            state.icalEntry.description?.let {
                Markdown(it, modifier = Modifier.fillMaxWidth())
            }

            AnimatedVisibility(state.icalEntry.url != null) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    UrlCard(
                        url = state.icalEntry.url ?: Url(""),
                        allowEditing = false,
                        onClick = { TODO() },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            AnimatedVisibility(state.icalEntry.location != null) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    LocationCard(
                        location = state.icalEntry.location ?: "",
                        allowEditing = false,
                        onClick = { TODO() },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            AnimatedVisibility(state.icalEntry.alarms.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    state.icalEntry.alarms.forEach { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            icalEntry = state.icalEntry,
                            allowEditing = false,
                            onAction = { /* no-op */ }
                        )
                    }
                }
            }

            AnimatedVisibility(state.icalEntry.attachments.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    state.icalEntry.attachments.forEach { attachment ->
                        AttachmentCard(
                            attachment = attachment,
                            allowEditing = false,
                            onAction = { TODO() },
                            isDownloading = false   // TODO state.downloadingAttachmentUids.contains(attachment.uid)
                        )
                    }
                }
            }

            //HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp))
            //WavyHorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

            val sortedSubtasks = state.subtasks.sortedWith(
                compareBy<IcalEntry> { if (it.syncState.isDeletedState()) 1 else 0 }
                    .thenBy { it.orderNo ?: it.created.instant.toEpochMilliseconds() }
            )
            val activeSortedSubtasks = sortedSubtasks.filter { it.syncState.isDeletedState() }

            AnimatedVisibility(activeSortedSubtasks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    activeSortedSubtasks.forEach { subtask ->
                        TaskListItem(
                            icalEntry = subtask,
                            isSelected = false,
                            allowEditing = false,
                            deletePending = false,
                            onClick = { /* TODO */ },
                            onLongClick = {},
                            onToggleProgress = {
                                /*
                                onAction(
                                    PreviewAction.OnUpdateSubtaskProgress(
                                        if (subtask.getProgressTriState() == ToggleableState.On) 0L else 100L,
                                        subtask.id
                                    )
                                )
                                 */
                            },
                            onSwipeToDelete = { /* no-op */ }
                        )
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun ListScreen_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DetailsScreenPreview(
                state = DetailsState(
                    icalEntry = IcalEntry.getSampleIcalEntry().copy(description = IcalEntry.smapleMarkdownText)
                ),
                onAction = {}
            )
        }
    }
}

@Preview
@Composable
private fun ListScreen_recurring_readonly_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DetailsScreenPreview(
                state = DetailsState(
                    icalEntry = IcalEntry.getSampleJournal().copy(
                        extraProperties = listOf(RawIcsProperty(name = "RRULE", unfoldedLine = "RRULE:FREQ=WEEKLY"))
                    ),
                ),
                onAction = {},
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Preview
@Composable
private fun ListScreen_with_dtstart_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DetailsScreenPreview(
                state = DetailsState(
                    icalEntry = IcalEntry.getSampleIcalEntry().copy(
                        dtStart = IcsDateTime.nowFloating(),
                        url = Url("https://spectacled.techbee.at"),
                        attachments = listOf(Attachment(fileName = "test.pdf", size = 128000))
                    )
                ),
                onAction = {}
            )
        }
    }
}

@Preview
@Composable
private fun ListScreen_with_dtstart_and_timezone_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            DetailsScreenPreview(
                state = DetailsState(
                    icalEntry = IcalEntry.getSampleJournal().copy(
                        dtStart = IcsDateTime.nowInZone(TimeZone.of("Europe/Vienna")),
                        status = Status.DRAFT
                    ),
                ),
                onAction = {},
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}

@Preview
@Composable
private fun ListScreen_task_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            DetailsScreenPreview(
                state = DetailsState(
                    icalEntry = IcalEntry.getSampleIcalEntry().copy(
                        calendarComponent = CalendarComponent.VTODO,
                        dtStart = IcsDateTime.nowInZone(TimeZone.of("Europe/Vienna")),
                        status = Status.IN_PROCESS,
                        percentComplete = 50
                    ),
                    subtasks = listOf(
                        IcalEntry.getSampleTask(),
                        IcalEntry.getSampleTask(),
                        IcalEntry.getSampleTask()
                    )
                ),
                onAction = {},
                modifier = Modifier.fillMaxHeight()
            )
        }
    }
}



