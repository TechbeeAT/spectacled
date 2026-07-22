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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.screens.core.presentation.components.WavyHorizontalDivider
import at.techbee.spectacled.screens.details.presentation.components.AttachmentCard
import at.techbee.spectacled.screens.details.presentation.components.DateTimeCard
import at.techbee.spectacled.screens.details.presentation.components.UrlCard
import at.techbee.spectacled.screens.list.presentation.components.MetaInfoCard
import at.techbee.spectacled.screens.list.presentation.components.TaskListItem
import io.ktor.http.Url
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableColumn
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start
import spectacled.shared.generated.resources.description
import spectacled.shared.generated.resources.drag_handle
import spectacled.shared.generated.resources.summary


@Composable
fun DetailsScreen(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier,
    onEditorFocusChanged: (Boolean) -> Unit = {}
) {
    var summaryIsFocused by rememberSaveable { mutableStateOf(false) }
    var descriptionIsFocused by rememberSaveable { mutableStateOf(false) }

    // Report whether a text field is being edited so the caller can hide the FAB, which would
    // otherwise sit right above the keyboard and cover the field (SwiftUI resizes the view on iOS).
    val isEditorFocused = summaryIsFocused || descriptionIsFocused
    LaunchedEffect(isEditorFocused) { onEditorFocusChanged(isEditorFocused) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    var summaryLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var descriptionLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    // removes the keyboard when user scrolls to the top
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // available.y > 0 means pulling DOWN.
                // If we are at the top (scrollState.value <= 0) and pulling more down
                if (available.y > 0 && scrollState.value <= 0) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
                return Offset.Zero
            }
        }
    }


    SelectionContainer {
        Column(
            modifier = modifier
                .nestedScroll(nestedScrollConnection)
                .verticalScroll(scrollState)
        ) {

            if (state.icalEntry.isTask() || state.icalEntry.isJournal()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.icalEntry.isJournal()) {
                        DateTimeCard(
                            icsDateTime = state.icalEntry.dtStart,
                            enabled = state.allowEditing(),
                            allowNoDate = false,
                            initializeWithDateOnly = true,
                            suggestedTimezones = state.latestUsedTimezones,
                            onIcsDateTimeUpdated = { onAction(DetailsAction.OnUpdateDtStart(it)) }
                        )

                    } else if (state.icalEntry.isTask()) {
                        DateTimeCard(
                            icsDateTime = state.icalEntry.dtStart,
                            enabled = state.allowEditing(),
                            allowNoDate = true,
                            initializeWithDateOnly = state.icalEntry.due?.isDateOnly != false,  // only when due is NOT date only we initialize with time
                            suggestedTimezones = state.latestUsedTimezones,
                            onIcsDateTimeUpdated = { onAction(DetailsAction.OnUpdateDtStart(it)) },
                            headerText = stringResource(Res.string.date_start),
                            selectableDates = if (state.icalEntry.due == null) DatePickerDefaults.AllDates else object : SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                    return utcTimeMillis < state.icalEntry.due.toDatePickerMillis(TimeZone.currentSystemDefault())
                                }
                            }
                        )

                        DateTimeCard(
                            icsDateTime = state.icalEntry.due,
                            enabled = state.allowEditing(),
                            allowNoDate = true,
                            initializeWithDateOnly = state.icalEntry.dtStart?.isDateOnly != false,  // only when dtStart is NOT date only we initialize with time
                            suggestedTimezones = state.latestUsedTimezones,
                            onIcsDateTimeUpdated = { onAction(DetailsAction.OnUpdateDue(it)) },
                            headerText = stringResource(Res.string.date_due),
                            selectableDates = if (state.icalEntry.dtStart == null) DatePickerDefaults.AllDates else object :
                                SelectableDates {
                                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                    return utcTimeMillis > state.icalEntry.dtStart.toDatePickerMillis(TimeZone.currentSystemDefault())
                                }
                            }
                        )
                    }

                    //HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp))
                    WavyHorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                }
            }

            AnimatedVisibility(
                state.icalEntry.categories.isNotEmpty() || state.icalEntry.status in listOf(
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
                            icon = state.icalEntry.status?.vectorIcon ?: Status.DRAFT.vectorIcon!!,
                            iconContentDescription = stringResource(state.icalEntry.status?.stringRes ?: Status.DRAFT.stringRes),
                            text = stringResource(state.icalEntry.status?.stringRes ?: Status.DRAFT.stringRes)
                        )
                    }

                    state.icalEntry.categories.sorted().forEach { category ->

                        MetaInfoCard(
                            icon = Icons.AutoMirrored.Outlined.Label,
                            iconContentDescription = stringResource(Res.string.category),
                            text = category,
                            onClick = { onAction(DetailsAction.OnShowCategorySelectorBottomSheet(true)) }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
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
                    onTextLayout = { summaryLayoutResult = it },
                    visualTransformation = MarkdownVisualTransformation(
                        localContentColor = LocalContentColor.current,
                        linkColor = MaterialTheme.colorScheme.primary
                    ),
                    cursorBrush = SolidColor(LocalContentColor.current),
                    modifier = Modifier
                        .onFocusChanged { summaryIsFocused = it.isFocused }
                        .weight(1f)
                        .pointerInput(uriHandler) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.changes.any { it.changedToUp() }) {
                                        val position = event.changes.first().position
                                        summaryLayoutResult?.let { layoutResult ->
                                            val offset = layoutResult.getOffsetForPosition(position)
                                            val text = layoutResult.layoutInput.text
                                            val range = text.getLinkAnnotations(0, text.length)
                                                .firstOrNull { offset >= it.start && offset < it.end }

                                            if (range != null) {
                                                (range.item as? LinkAnnotation.Url)?.let { urlAnnotation ->
                                                    try {
                                                        uriHandler.openUri(urlAnnotation.url)
                                                        event.changes.forEach { it.consume() }
                                                    } catch (_: Throwable) {
                                                        // ignore
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                )

                if (state.icalEntry.isTask()) {
                    val entryTriState = state.icalEntry.getProgressTriState()
                    TriStateCheckbox(
                        state = entryTriState,
                        onClick = { onAction(DetailsAction.OnUpdateProgress(if (entryTriState == ToggleableState.On) 0 else 100)) }
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
                onTextLayout = { descriptionLayoutResult = it },
                visualTransformation = MarkdownVisualTransformation(
                    localContentColor = LocalContentColor.current,
                    linkColor = MaterialTheme.colorScheme.primary
                ),
                cursorBrush = SolidColor(LocalContentColor.current),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .onFocusChanged {
                        descriptionIsFocused = it.isFocused
                    }
                    .pointerInput(uriHandler) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.changedToUp() }) {
                                    val position = event.changes.first().position
                                    descriptionLayoutResult?.let { layoutResult ->
                                        val offset = layoutResult.getOffsetForPosition(position)
                                        val text = layoutResult.layoutInput.text
                                        val range = text.getLinkAnnotations(0, text.length)
                                            .firstOrNull { offset >= it.start && offset < it.end }

                                        if (range != null) {
                                            (range.item as? LinkAnnotation.Url)?.let { urlAnnotation ->
                                                try {
                                                    uriHandler.openUri(urlAnnotation.url)
                                                    event.changes.forEach { it.consume() }
                                                } catch (_: Throwable) {
                                                    // ignore
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
            )



            AnimatedVisibility(state.icalEntry.url != null) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    UrlCard(
                        url = state.icalEntry.url ?: Url(""),
                        onClick = onAction,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
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
                            onAction = onAction,
                            isDownloading = state.downloadingAttachmentUids.contains(attachment.uid)
                        )
                    }
                }
            }

            //HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp))
            WavyHorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))

            val sortedSubtasks = state.subtasks.sortedBy { it.orderNo ?: it.created.instant.toEpochMilliseconds() }


            ReorderableColumn(
                list = sortedSubtasks,
                onSettle = { fromIndex, toIndex ->
                    onAction(
                        DetailsAction.OnPersistOrderNo(
                            sortedSubtasks.toMutableList().apply {
                            add(toIndex, removeAt(fromIndex))
                        }.map { it.id }
                    ))
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) { _, subtask, isDragging ->

                key(subtask.id) {
                    ReorderableItem {
                        TaskListItem(
                            subtask,
                            isDragging,
                            onClick = { onAction(DetailsAction.OnNavigateToIcalEntryId(subtask.id)) },
                            onLongClick = {},
                            onToggleProgress = {
                                onAction(
                                    DetailsAction.OnUpdateSubtaskProgress(
                                        if (subtask.getProgressTriState() == ToggleableState.On) 0L else 100L,
                                        subtask.id
                                    )
                                )
                            },
                            onFilterCategory = {},
                            dragHandle = {
                                IconButton(
                                    onClick = {},
                                    modifier = with(this) {
                                        Modifier.draggableHandle()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DragIndicator,
                                        contentDescription = stringResource(Res.string.drag_handle)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(112.dp))  // scroll until all is above fab
        }
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
            icalEntry = IcalEntry.getSampleIcalEntry().copy(
                dtStart = IcsDateTime.now(),
                url = Url("https://spectacled.techbee.at"),
                attachments = listOf(Attachment(fileName = "test.pdf", size = 128000))
            ),
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
            originalIcalEntry = IcalEntry.getSampleIcalEntry(),
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


