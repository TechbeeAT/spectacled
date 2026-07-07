package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.list.presentation.ListAction
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.clear_selection
import spectacled.shared.generated.resources.search
import spectacled.shared.generated.resources.status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListFilterRow(
    listFilterCriteria: ListFilterCriteria,
    allCategories: List<String>,
    calendarComponent: CalendarComponent,
    onAction: (ListAction) -> Unit,
    modifier: Modifier = Modifier,
    searchBarFocusRequester: FocusRequester = remember { FocusRequester() }
) {

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }


    Column(modifier = modifier) {

        TextField(
            placeholder = { Text(stringResource(Res.string.search)) },
            value = listFilterCriteria.searchQuery ?: "",
            onValueChange = { onAction(ListAction.OnListFilterCriteriaChanged(listFilterCriteria.copy(searchQuery = it))) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .focusRequester(searchBarFocusRequester)
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {

            //Category
            ElevatedFilterChip(
                selected = !listFilterCriteria.searchCategory.isNullOrBlank(),
                onClick = {
                    categoryDropdownExpanded = !categoryDropdownExpanded
                },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Outlined.Label, stringResource(Res.string.category))
                },
                trailingIcon = {
                    if (!listFilterCriteria.searchCategory.isNullOrBlank())
                        IconButton(
                            onClick = {
                                onAction(ListAction.OnListFilterCriteriaChanged(listFilterCriteria.copy(searchCategory = null)))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Close, stringResource(Res.string.clear_selection))
                        }
                },
                label = {
                    Text(listFilterCriteria.searchCategory ?: stringResource(Res.string.category))

                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {

                        allCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onAction(ListAction.OnListFilterCriteriaChanged(listFilterCriteria.copy(searchCategory = category)))
                                    categoryDropdownExpanded = false
                                }
                            )
                        }

                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            //Status
            ElevatedFilterChip(
                selected = listFilterCriteria.filterStatus != null,
                onClick = {
                    statusDropdownExpanded = !statusDropdownExpanded
                },
                leadingIcon = {
                    Icon(Status.FINAL.vectorIcon!!, stringResource(Res.string.status))
                },
                trailingIcon = {
                    if (listFilterCriteria.filterStatus != null)
                        IconButton(
                            onClick = {
                                onAction(ListAction.OnListFilterCriteriaChanged(listFilterCriteria.copy(filterStatus = null)))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Close, stringResource(Res.string.clear_selection))
                        }
                },
                label = {
                    Text(listFilterCriteria.filterStatus?.stringRes?.let { stringResource(it) } ?: stringResource(Res.string.status))

                    DropdownMenu(
                        expanded = statusDropdownExpanded,
                        onDismissRequest = { statusDropdownExpanded = false }
                    ) {

                        Status.entriesForComponent(calendarComponent).forEach { status ->
                            DropdownMenuItem(
                                text = { Text(stringResource(status.stringRes)) },
                                onClick = {
                                    onAction(ListAction.OnListFilterCriteriaChanged(listFilterCriteria.copy(filterStatus = status)))
                                    statusDropdownExpanded = false
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun ListFilterRow_Preview() {

    ListFilterRow(
        listFilterCriteria = ListFilterCriteria(),
        allCategories = emptyList(),
        calendarComponent = CalendarComponent.VJOURNAL,
        onAction = { }
    )
}

@Preview
@Composable
fun ListFilterRow_search_and_category_Preview() {

    ListFilterRow(
        listFilterCriteria = ListFilterCriteria(searchQuery = "preview", searchCategory = "my category", filterStatus = Status.FINAL),
        allCategories = emptyList(),
        calendarComponent = CalendarComponent.VJOURNAL,
        onAction = { }
    )
}