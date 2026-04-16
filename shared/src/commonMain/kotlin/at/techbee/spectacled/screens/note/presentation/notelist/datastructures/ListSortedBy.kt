package at.techbee.spectacled.screens.note.presentation.notelist.datastructures

import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.created
import spectacled.shared.generated.resources.drag_and_drop
import spectacled.shared.generated.resources.last_modified
import spectacled.shared.generated.resources.summary

enum class ListSortedBy(
    val displayName: StringResource
) {
    CREATED(Res.string.created),
    LAST_MODIFIED(Res.string.last_modified),
    SUMMARY(Res.string.summary),
    DRAGANDDROP(Res.string.drag_and_drop)
}