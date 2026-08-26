package at.techbee.spectacled.screens.list.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DatasetLinked
import androidx.compose.material.icons.outlined.EditOff
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.Route
import at.techbee.spectacled.screens.Route.IcalEntryDetails
import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.getPlatform
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.components.ColorSelectorElement
import at.techbee.spectacled.screens.core.presentation.components.CustomBottomSnackbarHost
import at.techbee.spectacled.screens.core.presentation.components.DatePickerBottomSheet
import at.techbee.spectacled.screens.core.presentation.imeAwarePadding
import at.techbee.spectacled.screens.details.presentation.DetailsInitialAction
import at.techbee.spectacled.screens.details.presentation.components.CategorySelectionBottomSheet
import at.techbee.spectacled.screens.list.presentation.components.DeleteSelectedItemsDialog
import at.techbee.spectacled.screens.list.presentation.components.DeriveEntriesBottomSheet
import at.techbee.spectacled.screens.list.presentation.components.IcalEntryListTopBar
import at.techbee.spectacled.screens.list.presentation.components.ListFilterRow
import at.techbee.spectacled.screens.list.presentation.components.MoveSelectedItemsDialog
import at.techbee.spectacled.theme.getColorSchemeForSeedColor
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_attachment
import spectacled.shared.generated.resources.add_drawing
import spectacled.shared.generated.resources.add_from_gallery
import spectacled.shared.generated.resources.add_photo
import spectacled.shared.generated.resources.ai_create_entries
import spectacled.shared.generated.resources.ai_provider_not_configured
import spectacled.shared.generated.resources.done
import spectacled.shared.generated.resources.link_file_by_url
import spectacled.shared.generated.resources.more
import spectacled.shared.generated.resources.read_only
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreenRoot(
    listViewModel: ListViewModel,
    onNavigate: (Route) -> Unit,
    onNavigateUp: () -> Unit,
    removeSafeAreaPaddingValues: Boolean = false,
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

        if (state.showMoveSelectedItemsDialog && state.multiselectItems?.isNotEmpty() == true) {
            MoveSelectedItemsDialog(
                itemCount = state.multiselectItems?.size ?: 0,
                sourceCalendarId = state.calendar.id,
                principals = state.allPrincipals,
                homeCollections = state.allHomeCollections,
                calendars = state.allCalendars.filter { it.canWriteContent() },
                onConfirm = { targetCalendarId -> listViewModel.onAction(ListAction.OnMoveSelectedItems(targetCalendarId)) },
                onDismiss = { listViewModel.onAction(ListAction.OnShowMoveSelectedItemsDialog(false)) }
            )
        }

        if (state.showUpdateColorOfSelectedBottomSheet && state.multiselectItems?.isNotEmpty() == true) {
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

        if (state.showUpdateCategoryOfSelectedBottomSheet && state.multiselectItems?.isNotEmpty() == true) {
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

        if (state.showDeriveEntriesBottomSheet) {
            DeriveEntriesBottomSheet(
                aiDerivedEntriesResult = state.aiDerivedEntriesResult,
                allowSubtasks = state.calendar.isTasksSupported(),
                onCreate = { text, createSubtasks -> listViewModel.onAction(ListAction.OnDeriveEntriesFromText(text, createSubtasks)) },
                onCreateWithoutAi = { text ->
                    onNavigate(IcalEntryDetails(icalEntryId = 0L, newIcalEntryCalendarId = state.calendar.id, newIcalEntryInitialDescription = text))
                    listViewModel.onAction(ListAction.OnShowDeriveEntriesBottomSheet(false))
                },
                onDismiss = { listViewModel.onAction(ListAction.OnShowDeriveEntriesBottomSheet(false)) }
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
                    removeHorizontalWindowInsets = removeSafeAreaPaddingValues,
                    modifier = Modifier.fillMaxWidth(1f)
                )
            },
            floatingActionButton = {
                // only shown if multiselect is null
                AnimatedVisibility(state.multiselectItems == null) {
                    if (state.calendar.canWriteContent()) {
                        ExtendedFloatingActionButton(
                            onClick = { },
                            // Lift the FAB by the bottom safe-area inset so it clears the iOS home
                            // indicator (the content still fills to the edge, only the FAB is padded).
                            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom)),
                        ) {

                            var fabMoreExpanded by remember { mutableStateOf(false) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { onNavigate(IcalEntryDetails(0L, state.calendar.id)) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(listViewModel.spectacledVariant.addNewDrawableRes),
                                            contentDescription = stringResource(listViewModel.spectacledVariant.addNewStringRes)
                                        )

                                        Text(
                                            text = stringResource(listViewModel.spectacledVariant.addNewStringRes)
                                        )
                                    }
                                }

                                VerticalDivider(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .padding(horizontal = 4.dp),
                                    color = IconButtonDefaults.iconButtonColors().contentColor
                                )

                                IconButton(
                                    onClick = { fabMoreExpanded = !fabMoreExpanded }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.MoreVert,
                                        contentDescription = stringResource(Res.string.more)
                                    )

                                    DropdownMenu(
                                        expanded = fabMoreExpanded,
                                        onDismissRequest = { fabMoreExpanded = false }
                                    ) {

                                        // AI "create entries from text" - only when an Anthropic key is set and the
                                        // collection is writable.
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(stringResource(Res.string.ai_create_entries))
                                                    if (!state.isAiProviderConfigured)
                                                        Text(
                                                            text = stringResource(Res.string.ai_provider_not_configured),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontStyle = FontStyle.Italic
                                                        )
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.AutoAwesome, stringResource(Res.string.ai_create_entries)) },
                                            enabled = state.isAiProviderConfigured,
                                            onClick = {
                                                listViewModel.onAction(ListAction.OnShowDeriveEntriesBottomSheet(true))
                                                fabMoreExpanded = false
                                            },
                                        )

                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.add_attachment)) },
                                            leadingIcon = { Icon(Icons.Outlined.Attachment, stringResource(Res.string.add_attachment)) },
                                            onClick = {
                                                onNavigate(IcalEntryDetails(0L, state.calendar.id, detailsInitialAction = DetailsInitialAction.ADD_ATTACHMENT.name))
                                                fabMoreExpanded = false
                                            },
                                        )

                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.link_file_by_url)) },
                                            leadingIcon = { Icon(Icons.Outlined.DatasetLinked, stringResource(Res.string.link_file_by_url)) },
                                            onClick = {
                                                onNavigate(IcalEntryDetails(0L, state.calendar.id, detailsInitialAction = DetailsInitialAction.ADD_ATTACHMENT_URL.name))
                                                fabMoreExpanded = false
                                            },
                                        )

                                        if (getPlatform().platform in listOf(Platforms.IOS, Platforms.ANDROID)) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(Res.string.add_photo)) },
                                                leadingIcon = { Icon(Icons.Outlined.PhotoCamera, stringResource(Res.string.add_photo)) },
                                                onClick = {
                                                    onNavigate(IcalEntryDetails(0L, state.calendar.id, detailsInitialAction = DetailsInitialAction.ADD_PHOTO.name))
                                                    fabMoreExpanded = false
                                                },
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.add_from_gallery)) },
                                            leadingIcon = { Icon(Icons.Outlined.Image, stringResource(Res.string.add_from_gallery)) },
                                            onClick = {
                                                onNavigate(IcalEntryDetails(0L, state.calendar.id, detailsInitialAction = DetailsInitialAction.ADD_FROM_GALLERY.name))
                                                fabMoreExpanded = false
                                            },
                                        )


                                        DropdownMenuItem(
                                            text = { Text(stringResource(Res.string.add_drawing)) },
                                            leadingIcon = { Icon(Icons.Outlined.Gesture, stringResource(Res.string.add_drawing)) },
                                            onClick = {
                                                onNavigate(IcalEntryDetails(0L, state.calendar.id, detailsInitialAction = DetailsInitialAction.ADD_DRAWING.name))
                                                fabMoreExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        ExtendedFloatingActionButton(
                            onClick = { listViewModel.onAction(ListAction.OnUpdateSnackbar("No write access to this collection")) },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
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
            // Drop the bottom safe-area inset so the content fills to the screen edge and there
            // is no empty strip (the "white gap") over the iOS home indicator. In landscape, we
            // additionally drop the horizontal insets so the content fills the sides; the top bar
            // keeps its own vertical insets in either case.
            contentWindowInsets = if (removeSafeAreaPaddingValues)
                ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
            else
                ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            modifier = modifier
        ) { paddingValues ->

            Box(
                modifier = Modifier
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .imeAwarePadding()
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
                                ListScreenJournals(
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