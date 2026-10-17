package at.techbee.spectacled.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.list.presentation.datastructures.ListConfiguration

/*
 * Persistence of the widget's filter criteria in the Glance widget state.
 *
 * Only the criteria offered by the widget configuration are stored; ListFilterCriteria.searchQuery
 * belongs to the transient search of the list screen and stays at its default here.
 */

private val CATEGORY_KEY = stringPreferencesKey("filter_category")
private val STATUS_KEY = stringPreferencesKey("filter_status")
private const val STATUS_NO_STATUS = "NO_STATUS"
private val HIDE_COMPLETED_TASKS_KEY = booleanPreferencesKey("filter_hide_completed_tasks")

/** The filter criteria stored for a widget, all defaults for a widget configured before filtering existed. */
fun Preferences.getListFilterCriteria() = ListConfiguration(
    // Escaped list (see ListFilterCriteria.joinEscaped); a widget from before multiselect stored a
    // single category, which reads back unchanged unless it contained a comma or backslash.
    searchCategories = this[CATEGORY_KEY]?.let { ListConfiguration.splitEscaped(it) } ?: emptyList(),
    // An unknown name means the enum changed since the widget was configured: drop that entry.
    filterStatus = this[STATUS_KEY]?.let { stored ->
        ListConfiguration.splitEscaped(stored).flatMap<String, Status?> { storedStatus ->
            if (storedStatus == STATUS_NO_STATUS)
                listOf(null)
            else
                listOfNotNull(Status.entries.firstOrNull { it.name == storedStatus })
        }
    } ?: emptyList(),
    hideCompletedTasks = this[HIDE_COMPLETED_TASKS_KEY] == true
)

fun MutablePreferences.setListFilterCriteria(listConfiguration: ListConfiguration) {
    val categories = listConfiguration.searchCategories
    if (categories.isEmpty())
        remove(CATEGORY_KEY)
    else
        this[CATEGORY_KEY] = ListConfiguration.joinEscaped(categories)

    val status = listConfiguration.filterStatus
    if (status.isEmpty())
        remove(STATUS_KEY)
    else
        this[STATUS_KEY] = ListConfiguration.joinEscaped(status.map { it?.name ?: STATUS_NO_STATUS })

    this[HIDE_COMPLETED_TASKS_KEY] = listConfiguration.hideCompletedTasks
}
