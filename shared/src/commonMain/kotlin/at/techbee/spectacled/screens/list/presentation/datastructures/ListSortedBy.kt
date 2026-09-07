package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.SpectacledVariant
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.created
import spectacled.shared.generated.resources.date
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start
import spectacled.shared.generated.resources.drag_and_drop
import spectacled.shared.generated.resources.ic_sorted_by_created
import spectacled.shared.generated.resources.ic_sorted_by_date
import spectacled.shared.generated.resources.ic_sorted_by_draganddrop
import spectacled.shared.generated.resources.ic_sorted_by_due
import spectacled.shared.generated.resources.ic_sorted_by_last_modified
import spectacled.shared.generated.resources.ic_sorted_by_start
import spectacled.shared.generated.resources.ic_sorted_by_summary
import spectacled.shared.generated.resources.last_modified
import spectacled.shared.generated.resources.summary

enum class ListSortedBy(
    val displayName: StringResource,
    val defaultAsc: Boolean,
    val displayIcon: DrawableResource
) {
    CREATED(Res.string.created, false, Res.drawable.ic_sorted_by_created),
    LAST_MODIFIED(Res.string.last_modified, false, Res.drawable.ic_sorted_by_last_modified),
    DATE(Res.string.date, false, Res.drawable.ic_sorted_by_date),
    START(Res.string.date_start, false, Res.drawable.ic_sorted_by_start),
    DUE(Res.string.date_due, false, Res.drawable.ic_sorted_by_due),
    SUMMARY(Res.string.summary, true, Res.drawable.ic_sorted_by_summary),
    DRAGANDDROP(Res.string.drag_and_drop, true, Res.drawable.ic_sorted_by_draganddrop);

    companion object {
        fun entriesFor(spectacledVariant: SpectacledVariant): List<ListSortedBy> {
            return when (spectacledVariant) {
                SpectacledVariant.JOURNALS -> listOf(DATE, CREATED, LAST_MODIFIED, SUMMARY)
                SpectacledVariant.NOTES -> listOf(CREATED, LAST_MODIFIED, SUMMARY, DRAGANDDROP)
                SpectacledVariant.TASKS -> listOf(CREATED, LAST_MODIFIED, START, DUE, SUMMARY, DRAGANDDROP)  // TODO
            }
        }
    }
}