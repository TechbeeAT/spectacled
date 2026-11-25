package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.domain.Status
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

@Serializable
data class ListFilterCriteria(
    @Transient val searchQuery: String? = null,
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
        val mapperJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
        }
    }
}
