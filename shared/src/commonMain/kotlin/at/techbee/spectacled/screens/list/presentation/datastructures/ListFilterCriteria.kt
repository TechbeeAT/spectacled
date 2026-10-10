package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.domain.Status

data class ListFilterCriteria(
    val searchQuery: String? = null,
    val searchCategories: List<String> = emptyList(),
    /** A `null` element stands for entries without a status. */
    val filterStatus: List<Status?> = emptyList(),
    val hideCompletedTasks: Boolean = false
) {

    fun anyFilterActive() =
        searchQuery != null
                || searchCategories.isNotEmpty()
                || filterStatus.isNotEmpty()
                || hideCompletedTasks

    /** Categories are compared ignoring case, like CalDAV clients that differ only in capitalisation. */
    fun isCategorySelected(category: String) =
        searchCategories.any { it.equals(category, ignoreCase = true) }

    /** Removes [category] (in any capitalisation) if selected, otherwise adds it. */
    fun toggleCategory(category: String) = copy(
        searchCategories = if (isCategorySelected(category))
            searchCategories.filterNot { it.equals(category, ignoreCase = true) }
        else
            searchCategories + category
    )

    /** Removes [status] if selected, otherwise adds it; `null` toggles "no status". */
    fun toggleStatus(status: Status?) = copy(
        filterStatus = if (status in filterStatus)
            filterStatus - status
        else
            filterStatus + status
    )

    companion object {
        private const val SEPARATOR = ','
        private const val ESCAPE = '\\'

        /**
         * Joins [values] into one string separated by commas, escaping commas and backslashes inside
         * a value, so that [splitEscaped] restores the list exactly (categories may contain commas).
         */
        fun joinEscaped(values: List<String>): String =
            values.joinToString(separator = SEPARATOR.toString()) { value ->
                value.replace("$ESCAPE", "$ESCAPE$ESCAPE").replace("$SEPARATOR", "$ESCAPE$SEPARATOR")
            }

        /** Reverse of [joinEscaped]. A string without escapes or separators yields a single value. */
        fun splitEscaped(joined: String): List<String> = buildList {
            val current = StringBuilder()
            var escaped = false
            for (char in joined) {
                when {
                    escaped -> { current.append(char); escaped = false }
                    char == ESCAPE -> escaped = true
                    char == SEPARATOR -> { add(current.toString()); current.clear() }
                    else -> current.append(char)
                }
            }
            if (escaped)
                current.append(ESCAPE)   // a dangling escape is kept literally
            add(current.toString())
        }
    }
}
