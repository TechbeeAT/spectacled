package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.domain.Status

data class ListFilterCriteria(
    val searchQuery: String? = null,
    val searchCategories: List<String> = emptyList(),
    val filterStatus: List<Status?> = emptyList(),
    val hideCompletedTasks: Boolean = false
) {

    fun anyFilterActive() =
        searchQuery != null
                || searchCategories.isNotEmpty()
                || filterStatus.isNotEmpty()
                || hideCompletedTasks
}
