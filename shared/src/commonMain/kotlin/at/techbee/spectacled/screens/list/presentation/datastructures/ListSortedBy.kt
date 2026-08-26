package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.SpectacledVariant
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.created
import spectacled.shared.generated.resources.date
import spectacled.shared.generated.resources.date_due
import spectacled.shared.generated.resources.date_start
import spectacled.shared.generated.resources.drag_and_drop
import spectacled.shared.generated.resources.last_modified
import spectacled.shared.generated.resources.summary

enum class ListSortedBy(
    val displayName: StringResource,
    val defaultAsc: Boolean
) {
    CREATED(Res.string.created, false),
    LAST_MODIFIED(Res.string.last_modified, false),
    DATE(Res.string.date, false),
    START(Res.string.date_start, false),
    DUE(Res.string.date_due, false),
    SUMMARY(Res.string.summary, true),
    DRAGANDDROP(Res.string.drag_and_drop, true);

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