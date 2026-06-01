package at.techbee.spectacled.screens.details.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.screens.details.presentation.components.DateSelectionRow
import at.techbee.spectacled.screens.list.presentation.components.MetaInfoCard
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start
import spectacled.shared.generated.resources.description
import spectacled.shared.generated.resources.summary


@Composable
fun DetailsScreen(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var summaryIsFocused by rememberSaveable { mutableStateOf(false) }
    var descriptionIsFocused by rememberSaveable { mutableStateOf(false) }


    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {

        if(state.icalEntry.isJournal()) {
            DateSelectionRow(
                icsDateTime = state.icalEntry.dtStart,
                enabled = state.allowEditing(),
                allowNoDate = false,
                initializeWithDateOnly = true,
                iconColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary,
                suggestedTimezones = state.latestUsedTimezones,
                onIcsDateTimeUpdated = { onAction(DetailsAction.OnUpdateDtStart(it)) }
            )

            HorizontalDivider(modifier = Modifier.padding(4.dp))

        } else if (state.icalEntry.isTask()) {
            DateSelectionRow(
                icsDateTime = state.icalEntry.dtStart,
                enabled = state.allowEditing(),
                allowNoDate = true,
                initializeWithDateOnly = state.icalEntry.due?.isDateOnly != false,  // only when due is NOT date only we initialize with time
                iconColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary,
                suggestedTimezones = state.latestUsedTimezones,
                onIcsDateTimeUpdated = { onAction(DetailsAction.OnUpdateDtStart(it)) },
                headerText = stringResource(Res.string.date_start), 
                selectableDates = if (state.icalEntry.due == null) DatePickerDefaults.AllDates else object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis < state.icalEntry.due.toDatePickerMillis(TimeZone.currentSystemDefault())
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(4.dp))

            DateSelectionRow(
                icsDateTime = state.icalEntry.due,
                enabled = state.allowEditing(),
                allowNoDate = true,
                initializeWithDateOnly = state.icalEntry.dtStart?.isDateOnly != false,  // only when dtStart is NOT date only we initialize with time
                iconColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary,
                suggestedTimezones = state.latestUsedTimezones,
                onIcsDateTimeUpdated = { onAction(DetailsAction.OnUpdateDue(it)) },
                headerText = stringResource(Res.string.date_due),
                selectableDates = if (state.icalEntry.dtStart == null) DatePickerDefaults.AllDates else object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis > state.icalEntry.dtStart.toDatePickerMillis(TimeZone.currentSystemDefault())
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(4.dp))

        }

        AnimatedVisibility(state.icalEntry.categories.isNotEmpty() || state.icalEntry.status in listOf(Status.DRAFT, Status.CANCELLED)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {

                AnimatedVisibility(state.icalEntry.status in listOf(Status.DRAFT, Status.CANCELLED)) {
                    MetaInfoCard(
                        icon = state.icalEntry.status?.vectorIcon ?: Status.DRAFT.vectorIcon!!,
                        iconContentDescription = stringResource(state.icalEntry.status?.stringRes ?: Status.DRAFT.stringRes),
                        text = stringResource(state.icalEntry.status?.stringRes ?: Status.DRAFT.stringRes),
                        containerColor = state.icalEntry.color ?: Color.Unspecified,
                    )
                }

                state.icalEntry.categories.sorted().forEach { category ->

                    MetaInfoCard(
                        icon = Icons.AutoMirrored.Outlined.Label,
                        iconContentDescription = stringResource(Res.string.category),
                        containerColor = state.icalEntry.color ?: Color.Unspecified,
                        text = category,
                        onClick = { onAction(DetailsAction.OnShowCategorySelectorBottomSheet(true)) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {

            BasicTextField(
                value = if (!summaryIsFocused && state.icalEntry.summary.isNullOrEmpty()) stringResource(Res.string.summary) else state.icalEntry.summary
                    ?: "",
                onValueChange = {
                    onAction(DetailsAction.OnUpdateSummary(it))
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = if (!summaryIsFocused && state.icalEntry.summary.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
                ),
                enabled = state.allowEditing(),
                visualTransformation = MarkdownVisualTransformation(LocalContentColor.current),
                modifier = Modifier.onFocusChanged {
                        summaryIsFocused = it.isFocused
                    }
                    .weight(1f)
            )

            if(state.icalEntry.isTask()) {
                TriStateCheckbox(
                    state = state.icalEntry.getProgressTriState(),
                    onClick = { onAction(DetailsAction.OnUpdateProgress(if(state.icalEntry.percentComplete < 100) 100 else 0))}
                )
            }
        }

        BasicTextField(
            value = if (!descriptionIsFocused && state.icalEntry.description.isNullOrEmpty()) stringResource(Res.string.description) else state.icalEntry.description
                ?: "",
            onValueChange = {
                onAction(DetailsAction.OnUpdateDescription(it))
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (!descriptionIsFocused && state.icalEntry.description.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            ),
            enabled = state.allowEditing(),
            visualTransformation = MarkdownVisualTransformation(LocalContentColor.current),
            modifier = Modifier
                .heightIn(min = 200.dp)
                .fillMaxWidth()
                .fillMaxHeight(1f)
                .onFocusChanged {
                    descriptionIsFocused = it.isFocused
                }
        )
    }



}


@Preview
@Composable
private fun ListScreen_Preview() {
    DetailsScreen(
        state = DetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry(),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun ListScreen_with_dtstart_Preview() {
    DetailsScreen(
        state = DetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry().copy(dtStart = IcsDateTime.now()),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun ListScreen_with_dtstart_and_timezone_Preview() {
    DetailsScreen(
        state = DetailsState(
            icalEntry = IcalEntry.getSampleJournal().copy(
                dtStart = IcsDateTime.now().copy(timeZone = TimeZone.of("Europe/Vienna")),
                status = Status.DRAFT
            ),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {},
        modifier = Modifier.fillMaxHeight()
    )
}

@Preview
@Composable
private fun ListScreen_task_Preview() {
    DetailsScreen(
        state = DetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry().copy(
                calendarComponent = CalendarComponent.VTODO,
                dtStart = IcsDateTime.now().copy(timeZone = TimeZone.of("Europe/Vienna")),
                status = Status.IN_PROCESS,
                percentComplete = 50
            ),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {},
        modifier = Modifier.fillMaxHeight()
    )
}


