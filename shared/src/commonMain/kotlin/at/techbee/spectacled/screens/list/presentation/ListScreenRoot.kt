package at.techbee.spectacled.screens.list.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.IcalEntryDetails
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.components.ColorSelectorElement
import at.techbee.spectacled.screens.core.presentation.components.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.core.presentation.components.DatePickerBottomSheet
import at.techbee.spectacled.screens.details.presentation.components.CategorySelectionBottomSheet
import at.techbee.spectacled.screens.list.presentation.components.DeleteSelectedItemsDialog
import at.techbee.spectacled.screens.list.presentation.components.IcalEntryListTopBar
import at.techbee.spectacled.screens.list.presentation.components.ListFilterRow
import at.techbee.spectacled.theme.getColorSchemeForSeedColor
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_journal
import spectacled.shared.generated.resources.add_note
import spectacled.shared.generated.resources.add_task
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.ic_add_journal
import spectacled.shared.generated.resources.ic_add_note
import spectacled.shared.generated.resources.ic_add_task
import spectacled.shared.generated.resources.read_only
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreenRoot(
    listViewModel: ListViewModel,
    onNavigate: (Route) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {

    val state by listViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pullToRefreshState = rememberPullToRefreshState()

    val searchBarFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val selectableDates = remember(state.icalEntries) {
        val allowedMillis = state.icalEntries.mapNotNull {
            it.dtStart?.toDatePickerMillis(TimeZone.currentSystemDefault())
        }.toSet()
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return allowedMillis.contains(utcTimeMillis)
            }
        }
    }

    MaterialTheme(colorScheme = getColorSchemeForSeedColor(state.calendar.color)) {


        LaunchedEffect(state.snackbarText) {
            state.snackbarText?.let { message ->
                snackbarHostState.showSnackbar(message)
                listViewModel.onAction(ListAction.OnUpdateSnackbar(null))
            }
        }

        LaunchedEffect(state.navigateUp) {
            if (state.navigateUp) {
                onNavigateUp()
                listViewModel.onAction(ListAction.OnNavigateUp(false))
            }
        }

        LaunchedEffect(state.navigateToIcalEntryId) {
            state.navigateToIcalEntryId?.let {
                onNavigate(IcalEntryDetails(it))
                listViewModel.onAction(ListAction.OnIcalEntryClicked(null))
            }
        }

        LaunchedEffect(state.isSearchBarExpanded) {
            if (state.isSearchBarExpanded) {
                delay(300.milliseconds)
                searchBarFocusRequester.requestFocus()
                keyboardController?.show()
            } else {
                keyboardController?.hide()
            }
        }

        if (state.showDeleteSelectedItemsDialog && state.multiselectItems?.isNotEmpty() == true) {
            DeleteSelectedItemsDialog(
                multiselectItems = state.multiselectItems?: emptyList(),
                onConfirm = { listViewModel.onAction(ListAction.OnDeleteSelectedItems) },
                onDismiss = { listViewModel.onAction(ListAction.OnShowDeleteSelectedItemsDialog(false)) }
            )
        }

        if (state.showUpdateColorOfSelectedBottomSheet) {
            BottomSheetWithMenu(
                onDismiss = { listViewModel.onAction(ListAction.OnShowUpdateColorOfSelectedBottomSheet(false)) },
                menuActionRight = {
                    TextButton(
                        onClick = { listViewModel.onAction(ListAction.OnShowUpdateColorOfSelectedBottomSheet(false)) }
                    ) {
                        Text(stringResource(Res.string.done))
                    }
                },
            ) {
                ColorSelectorElement(
                    recentColors = state.icalEntries
                        .mapNotNull { it.color }
                        .distinct(),
                    preselectedColor = state.icalEntries
                        .filter { note -> state.multiselectItems?.contains(note.id) == true }
                        .map { it.color }
                        .distinct()
                        .let { colorList -> if (colorList.size == 1) colorList.first() else Color.Transparent },
                    onColorChanged = { listViewModel.onAction(ListAction.OnUpdateColorOfSelected(it)) },
                    skipPartialSelection = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (state.showUpdateCategoryOfSelectedBottomSheet) {
            CategorySelectionBottomSheet(
                allCategories = state.allCategories.filter { it != IcalEntry.PINNED_CATEGORY },
                selectedCategories = state.multiselectItems?.let { selectedIds ->
                    val selectedEntries = state.icalEntries.filter { it.id in selectedIds }
                    if (selectedEntries.isEmpty()) return@let emptyList()

                    selectedEntries
                        .map { it.categories.toSet() }
                        .reduce { acc, categories -> acc.intersect(categories) }
                        .filter { it != IcalEntry.PINNED_CATEGORY }
                        .toList()
                } ?: emptyList(),
                onCategoryAdded = { listViewModel.onAction(ListAction.OnUpdateCategoryOfSelected(it, null)) },
                onCategoryRemoved = { listViewModel.onAction(ListAction.OnUpdateCategoryOfSelected(null, it)) },
                onDismiss = { listViewModel.onAction(ListAction.OnShowUpdateCategoryOfSelectedBottomSheet(false)) }
            )
        }

        if (state.showDateSelectorBottomSheet) {
            DatePickerBottomSheet(
                icsDateTime = IcsDateTime.now(),
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                allowNoDate = false,
                selectableDates = selectableDates,
                onDateSelected = { selectedDate -> listViewModel.onAction(ListAction.OnGoToSelectedDate(selectedDate)) },
                onDismiss = { listViewModel.onAction(ListAction.OnShowDateSelectorBottomSheet(false)) }
            )
        }


        Scaffold(
            topBar = {
                IcalEntryListTopBar(
                    state = state,
                    onAction = { action -> listViewModel.onAction(action) },
                    allSelectedPinned = state.multiselectItems?.all { selectedId ->
                        state.icalEntries.find { entry -> entry.id == selectedId }?.categories?.contains(IcalEntry.PINNED_CATEGORY) == true
                    } == true,
                    modifier = Modifier.fillMaxWidth(1f)
                )
            },
            floatingActionButton = {
                // only shown if multiselect is null
                if (state.multiselectItems == null) {
                    if (state.calendar.canWriteContent()) {
                        ExtendedFloatingActionButton(
                            onClick = { onNavigate(Route.AddICalEntry(state.calendar.id)) },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (listViewModel.spectacledVariant) {
                                            SpectacledVariant.JOURNALS -> Res.drawable.ic_add_journal
                                            SpectacledVariant.NOTES -> Res.drawable.ic_add_note
                                            SpectacledVariant.TASKS -> Res.drawable.ic_add_task
                                        }
                                    ),
                                    contentDescription = stringResource(
                                        when (listViewModel.spectacledVariant) {
                                            SpectacledVariant.JOURNALS -> Res.string.add_journal
                                            SpectacledVariant.NOTES -> Res.string.add_note
                                            SpectacledVariant.TASKS -> Res.string.add_task
                                        }
                                    )
                                )

                                Text(
                                    text = stringResource(
                                        when (listViewModel.spectacledVariant) {
                                            SpectacledVariant.JOURNALS -> Res.string.add_journal
                                            SpectacledVariant.NOTES -> Res.string.add_note
                                            SpectacledVariant.TASKS -> Res.string.add_task
                                        }
                                    )
                                )
                            }
                        }
                    } else {
                        ExtendedFloatingActionButton(
                            onClick = { listViewModel.onAction(ListAction.OnUpdateSnackbar("No write access to this collection")) },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EditOff,
                                    contentDescription = stringResource(Res.string.read_only)
                                )
                                Text(text = stringResource(Res.string.read_only))
                            }
                        }
                    }
                }
            },
            bottomBar = { },
            modifier = modifier
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imePadding()
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {

                Column(modifier = Modifier.fillMaxSize()) {

                    AnimatedVisibility(state.isSearchBarExpanded) {
                        ListFilterRow(
                            listFilterCriteria = state.listFilterCriteria,
                            allCategories = state.icalEntries.flatMap { it.categories }.distinct(),
                            calendarComponent = state.spectacledVariant.mainCalendarComponent,
                            onAction = { listViewModel.onAction(it) },
                            searchBarFocusRequester = searchBarFocusRequester
                        )
                    }

                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { listViewModel.onAction(ListAction.OnTriggerSync) },
                        state = pullToRefreshState,
                        indicator = {
                            if (state.draggingIcalEntryId != null) {  // don't show indicator when dragging to avoid conflict
                                return@PullToRefreshBox
                            } else {
                                Indicator(
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    isRefreshing = state.isRefreshing,
                                    state = pullToRefreshState,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {

                        when (listViewModel.spectacledVariant) {
                            SpectacledVariant.JOURNALS -> {
                                JournalsListJournals(
                                    state = state,
                                    onAction = { action -> listViewModel.onAction(action) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            SpectacledVariant.NOTES -> {
                                ListScreenNotes(
                                    state = state,
                                    dragAndDropList = listViewModel.dragAndDropList,
                                    onAction = { action -> listViewModel.onAction(action) },
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
                                )
                            }

                            SpectacledVariant.TASKS -> {
                                ListScreenTasks(
                                    state = state,
                                    dragAndDropList = listViewModel.dragAndDropList,
                                    onAction = { action -> listViewModel.onAction(action) },
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                }
                CustomBottomSnackbarHost(
                    snackbarHostState = snackbarHostState,
                    keepSpaceForFAB = true
                )
            }
        }
    }
}