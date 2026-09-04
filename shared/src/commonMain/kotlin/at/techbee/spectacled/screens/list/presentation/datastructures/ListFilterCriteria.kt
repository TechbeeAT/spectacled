package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status

data class ListFilterCriteria(
    val searchQuery: String? = null,
    val searchCategory: String? = null,
    val filterStatus: Status? = null,

    val hideCompletedTasks: Boolean = false
) {

    fun anyFilterActive() =
        searchQuery != null
                || searchCategory != null
                || filterStatus != null
                || hideCompletedTasks

    /**
     * Whether [icalEntry] passes all currently active criteria. Shared by the list screen and the
     * widget so both apply the filters in exactly the same way.
     */
    fun matches(icalEntry: IcalEntry): Boolean {
        val query = searchQuery
        val category = searchCategory

        val matchesQuery = query.isNullOrBlank()
                || icalEntry.summary?.contains(query, ignoreCase = true) == true
                || icalEntry.description?.contains(query, ignoreCase = true) == true

        if (!matchesQuery)
            return false

        val matchesCategory = category.isNullOrBlank()
                || icalEntry.categories.any { it.equals(category, ignoreCase = true) }

        if (!matchesCategory)
            return false

        if (filterStatus != null && icalEntry.status != filterStatus)
            return false

        if (hideCompletedTasks && icalEntry.isDone())
            return false

        return true
    }
}
