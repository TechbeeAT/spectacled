package at.techbee.spectacled.screens.list.presentation.components

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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

        //Category
        ElevatedFilterChip(
            selected = !listFilterCriteria.searchCategory.isNullOrBlank(),
            enabled = allCategories.isNotEmpty(),
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
                            onListFilterCriteriaChanged(listFilterCriteria.copy(searchCategory = null))
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
                                onListFilterCriteriaChanged(listFilterCriteria.copy(searchCategory = category))
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            },
            modifier = chipModifier
        )

        //Status
        ElevatedFilterChip(
            selected = listFilterCriteria.filterStatus != null,
            onClick = {
                statusDropdownExpanded = !statusDropdownExpanded
            },
            leadingIcon = { listFilterCriteria.filterStatus?.StatusIcon(0) ?: Status.DRAFT.StatusIcon(0) },
            trailingIcon = {
                if (listFilterCriteria.filterStatus != null)
                    IconButton(
                        onClick = {
                            onListFilterCriteriaChanged(listFilterCriteria.copy(filterStatus = null))
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
                                onListFilterCriteriaChanged(listFilterCriteria.copy(filterStatus = status))
                                statusDropdownExpanded = false
                            }
                        )
                    }
                }
            },
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
        listFilterCriteria = ListFilterCriteria(searchQuery = "preview", searchCategory = "my category", filterStatus = Status.FINAL),
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