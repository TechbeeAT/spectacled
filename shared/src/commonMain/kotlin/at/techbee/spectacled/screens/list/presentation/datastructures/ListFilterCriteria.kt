package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.domain.Status

data class ListFilterCriteria(
    val searchQuery: String? = null,
    val searchCategory: String? = null,
    val filterStatus: Status? = null
) {
    fun anyFilterActive() = searchQuery != null || searchCategory != null || filterStatus != null

    fun cleared() = ListFilterCriteria()
}