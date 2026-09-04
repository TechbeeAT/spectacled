package at.techbee.spectacled.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria

/*
 * Persistence of the widget's filter criteria in the Glance widget state.
 *
 * Only the criteria offered by the widget configuration are stored; ListFilterCriteria.searchQuery
 * belongs to the transient search of the list screen and stays at its default here.
 */

private val CATEGORY_KEY = stringPreferencesKey("filter_category")
private val STATUS_KEY = stringPreferencesKey("filter_status")
private val HIDE_COMPLETED_TASKS_KEY = booleanPreferencesKey("filter_hide_completed_tasks")

/** The filter criteria stored for a widget, all defaults for a widget configured before filtering existed. */
fun Preferences.getListFilterCriteria() = ListFilterCriteria(
    searchCategory = this[CATEGORY_KEY],
    // An unknown name means the enum changed since the widget was configured: treat it as no filter.
    filterStatus = this[STATUS_KEY]?.let { stored -> Status.entries.firstOrNull { it.name == stored } },
    hideCompletedTasks = this[HIDE_COMPLETED_TASKS_KEY] == true
)

fun MutablePreferences.setListFilterCriteria(listFilterCriteria: ListFilterCriteria) {
    val category = listFilterCriteria.searchCategory
    if (category.isNullOrBlank())
        remove(CATEGORY_KEY)
    else
        this[CATEGORY_KEY] = category

    val status = listFilterCriteria.filterStatus
    if (status == null)
        remove(STATUS_KEY)
    else
        this[STATUS_KEY] = status.name

    this[HIDE_COMPLETED_TASKS_KEY] = listFilterCriteria.hideCompletedTasks
}
