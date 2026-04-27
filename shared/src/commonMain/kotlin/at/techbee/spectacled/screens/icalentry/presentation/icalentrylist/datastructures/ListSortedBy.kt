package at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.datastructures

import at.techbee.spectacled.SpectacledVariant
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.created
import spectacled.shared.generated.resources.date
import spectacled.shared.generated.resources.drag_and_drop
import spectacled.shared.generated.resources.last_modified
import spectacled.shared.generated.resources.summary

enum class ListSortedBy(
    val displayName: StringResource
) {
    CREATED(Res.string.created),
    LAST_MODIFIED(Res.string.last_modified),
    DATE(Res.string.date),
    SUMMARY(Res.string.summary),
    DRAGANDDROP(Res.string.drag_and_drop);

    companion object {
        fun entriesFor(spectacledVariant: SpectacledVariant): List<ListSortedBy> {
            return when (spectacledVariant) {
                SpectacledVariant.JOURNALS -> listOf(DATE, CREATED, LAST_MODIFIED, SUMMARY)
                SpectacledVariant.NOTES -> listOf(CREATED, LAST_MODIFIED, SUMMARY, DRAGANDDROP)
                SpectacledVariant.TASKS -> listOf(CREATED, LAST_MODIFIED, SUMMARY, DRAGANDDROP)  // TODO
            }
        }
    }
}