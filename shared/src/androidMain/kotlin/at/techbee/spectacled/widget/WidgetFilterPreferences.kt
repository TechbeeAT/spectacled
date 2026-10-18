package at.techbee.spectacled.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria

/*
 * Persistence of the widget's filter criteria in the Glance widget state, as the same JSON the
 * list screen stores per calendar. ListFilterCriteria.searchQuery is transient and not stored.
 */

private val LIST_FILTER_CRITERIA_KEY = stringPreferencesKey("list_filter_criteria")

/** The filter criteria stored for a widget, all defaults if none (or none readable) are stored. */
fun Preferences.getListFilterCriteria(): ListFilterCriteria =
    this[LIST_FILTER_CRITERIA_KEY]?.let { json ->
        runCatching { ListFilterCriteria.mapperJson.decodeFromString<ListFilterCriteria>(json) }.getOrNull()
    } ?: ListFilterCriteria()

fun MutablePreferences.setListFilterCriteria(listFilterCriteria: ListFilterCriteria) {
    this[LIST_FILTER_CRITERIA_KEY] = ListFilterCriteria.mapperJson.encodeToString(listFilterCriteria)
}
