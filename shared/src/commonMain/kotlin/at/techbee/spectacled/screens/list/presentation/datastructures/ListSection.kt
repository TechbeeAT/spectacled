package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

/**
 * A single, ordered block of a list screen. Every list (notes, tasks, journals) is built from the
 * same sequence of sections computed once in [at.techbee.spectacled.screens.list.presentation.ListState.recompute]:
 *
 *   1. [Kind.PINNED]       – pinned entries
 *   2. [Kind.GROUPED]      – entries grouped by the active [ListSortedBy] selection (0..n sections)
 *   3. [Kind.NO_CRITERIA]  – entries that don't match the grouping criterion (no date) or that are
 *                            the "wrong" type for this app (a dated journal in the notes list, an
 *                            undated note in the journals list)
 *   4. [Kind.TRASHBIN]     – locally deleted entries
 *
 * Sections are plain data so they can be produced off the UI thread; header text is resolved at
 * render time from [header] (see the `listSections` helpers).
 *
 * @param key    stable identity used both as the collapse-preference key and the lazy-list item key
 *               prefix. Empty sections and `null` [header] sections are never collapsible.
 * @param dimmed render entries and header at reduced alpha (used for the trashbin).
 */
data class ListSection(
    val key: String,
    val header: ListSectionHeader?,
    val entries: List<IcalEntry>,
    val kind: Kind,
    val dimmed: Boolean = false,
) {
    enum class Kind { PINNED, GROUPED, NO_CRITERIA, TRASHBIN }
}

/** How a [ListSection]'s header should be rendered. */
sealed interface ListSectionHeader {

    /** A localized header, optionally formatted with [param], an [iconDrawableRes] for pinned or trashbin */
    data class Res(
        val stringRes: StringResource,
        val param: Int? = null,
        val iconDrawableRes: DrawableResource? = null
    ) : ListSectionHeader

    /** A pre-formatted, non-localized header such as a day label. */
    data class Raw(val text: String) : ListSectionHeader

    /** A journals month header, rendered as a sticky [at.techbee.spectacled.screens.list.presentation.components.MonthHeader]. */
    data class Month(val icsDateTime: IcsDateTime?) : ListSectionHeader
}
