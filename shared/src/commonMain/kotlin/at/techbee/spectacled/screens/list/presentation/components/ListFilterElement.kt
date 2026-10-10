package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.presentation.components.StatusWithProgressIcon
import at.techbee.spectacled.screens.core.presentation.horizontalFadingEdges
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.category
import spectacled.shared.generated.resources.clear_selection
import spectacled.shared.generated.resources.hide_completed_tasks
import spectacled.shared.generated.resources.ic_completed_hidden
import spectacled.shared.generated.resources.ic_completed_visible
import spectacled.shared.generated.resources.status
import spectacled.shared.generated.resources.status_no_status

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListFilterElement(
    listFilterCriteria: ListFilterCriteria,
    allCategories: List<String>,
    calendarComponent: CalendarComponent,
    onListFilterCriteriaChanged: (ListFilterCriteria) -> Unit,
    isVertical: Boolean = false,
    modifier: Modifier = Modifier,
) {

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }

    val chipModifier = if(isVertical) Modifier.fillMaxWidth().heightIn(min = 48.dp) else Modifier

    val chips = @Composable {

        //Category
        ElevatedFilterChip(
            selected = listFilterCriteria.searchCategories.isNotEmpty(),
            enabled = allCategories.isNotEmpty(),
            onClick = {
                categoryDropdownExpanded = !categoryDropdownExpanded
            },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Outlined.Label, stringResource(Res.string.category))
            },
            trailingIcon = {
                if (listFilterCriteria.searchCategories.isNotEmpty())
                    IconButton(
                        onClick = {
                            onListFilterCriteriaChanged(listFilterCriteria.copy(searchCategories = emptyList()))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Outlined.Close, stringResource(Res.string.clear_selection))
                    }
            },
            label = {
                Column {
                    Text(stringResource(Res.string.category))

                    AnimatedVisibility(listFilterCriteria.searchCategories.isNotEmpty()) {
                        Text(
                            listFilterCriteria.searchCategories.joinToString(separator = ", "),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {

                    allCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                onListFilterCriteriaChanged(listFilterCriteria.copy(
                                    searchCategories =
                                        if(listFilterCriteria.searchCategories.contains(category))
                                            listFilterCriteria.searchCategories.minus(category)
                                        else
                                            listFilterCriteria.searchCategories.plus(category)
                                ))
                            },
                            trailingIcon = {
                                if(listFilterCriteria.searchCategories.contains(category))
                                    Icon(Icons.Outlined.Check, null)
                            }
                        )
                    }
                }
            },
            modifier = chipModifier
        )

        //Status
        ElevatedFilterChip(
            selected = listFilterCriteria.filterStatus.isNotEmpty(),
            onClick = {
                statusDropdownExpanded = !statusDropdownExpanded
            },
            leadingIcon = { StatusWithProgressIcon(null, null) },
            trailingIcon = {
                if (listFilterCriteria.filterStatus.isNotEmpty())
                    IconButton(
                        onClick = {
                            onListFilterCriteriaChanged(listFilterCriteria.copy(filterStatus = emptyList()))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Outlined.Close, stringResource(Res.string.clear_selection))
                    }
            },
            label = {

                Column {
                    Text(stringResource(Res.string.status))

                    AnimatedVisibility(listFilterCriteria.filterStatus.isNotEmpty()) {
                        Text(
                            listFilterCriteria.filterStatus.map {
                                if (it == null) stringResource(Res.string.status_no_status) else stringResource(it.stringRes)
                            }.joinToString(separator = ", "),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                DropdownMenu(
                    expanded = statusDropdownExpanded,
                    onDismissRequest = { statusDropdownExpanded = false }
                ) {

                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.status_no_status)) },
                        onClick = {
                            onListFilterCriteriaChanged(listFilterCriteria.copy(
                                filterStatus =
                                    if(listFilterCriteria.filterStatus.contains(null))
                                        listFilterCriteria.filterStatus.minus(null)
                                    else
                                        listFilterCriteria.filterStatus.plus(null)
                            ))
                        },
                        trailingIcon = {
                            if(listFilterCriteria.filterStatus.contains(null))
                                Icon(Icons.Outlined.Check, null)
                        },
                        leadingIcon = {
                            StatusWithProgressIcon(null, null)
                        }
                    )

                    Status.entriesForComponent(calendarComponent).forEach { status ->
                        DropdownMenuItem(
                            text = { Text(stringResource(status.stringRes)) },
                            onClick = {
                                onListFilterCriteriaChanged(listFilterCriteria.copy(
                                    filterStatus =
                                        if(listFilterCriteria.filterStatus.contains(status))
                                            listFilterCriteria.filterStatus.minus(status)
                                        else
                                            listFilterCriteria.filterStatus.plus(status)
                                ))
                            },
                            trailingIcon = {
                                if(listFilterCriteria.filterStatus.contains(status))
                                    Icon(Icons.Outlined.Check, null)
                            },
                            leadingIcon = {
                                status.StatusIcon(null)
                            }
                        )
                    }
                }
            },
            modifier = chipModifier
        )

        //Hide completed
        ElevatedFilterChip(
            selected = listFilterCriteria.hideCompletedTasks,
            onClick = { onListFilterCriteriaChanged(listFilterCriteria.copy(hideCompletedTasks = !listFilterCriteria.hideCompletedTasks)) },
            leadingIcon = {
                Crossfade(listFilterCriteria.hideCompletedTasks) { hidden ->
                    if (hidden)
                        Icon(painterResource(Res.drawable.ic_completed_hidden), null)
                    else
                        Icon(painterResource(Res.drawable.ic_completed_visible), null)
                }
            },
            label = { Text(stringResource(Res.string.hide_completed_tasks)) },
            modifier = chipModifier
        )
    }


    Box(modifier = modifier) {

        if(isVertical) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                chips()
            }
        } else {
            val scrollState = rememberScrollState()
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdges(scrollState)
                    .horizontalScroll(scrollState)
            ) {
                chips()
            }
        }
    }
}

@Preview
@Composable
fun ListFilter_Element_Preview() {

    ListFilterElement(
        listFilterCriteria = ListFilterCriteria(),
        allCategories = emptyList(),
        calendarComponent = CalendarComponent.VJOURNAL,
        onListFilterCriteriaChanged = { }
    )
}

@Preview
@Composable
fun ListFilter_Element_search_and_category_Preview() {

    ListFilterElement(
        listFilterCriteria = ListFilterCriteria(searchQuery = "preview", searchCategories = listOf("my category"), filterStatus = listOf(Status.FINAL)),
        allCategories = emptyList(),
        calendarComponent = CalendarComponent.VJOURNAL,
        onListFilterCriteriaChanged = { }
    )
}

@Preview
@Composable
fun ListFilter_Column_Preview() {

    ListFilterElement(
        listFilterCriteria = ListFilterCriteria(),
        allCategories = emptyList(),
        calendarComponent = CalendarComponent.VJOURNAL,
        onListFilterCriteriaChanged = { },
        isVertical = true
    )
}