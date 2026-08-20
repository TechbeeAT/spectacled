package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSection
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSectionHeader

/**
 * [LazyVerticalStaggeredGrid] counterpart of [listSections] for the notes list. Headers span the
 * full line and collapse via [ListSectionHeader]; the trashbin section is dimmed. Only the per-entry
 * content differs between screens, so it is supplied via [itemContent].
 *
 * @param itemContent renders one entry; receives its section and its index within that section so
 *   the caller can compute first/last-in-section styling from `section.entries`.
 */
fun LazyStaggeredGridScope.listSections(
    sections: List<ListSection>,
    collapsedGroups: Set<String>,
    onToggleGroup: (String) -> Unit,
    itemContent: @Composable LazyStaggeredGridItemScope.(entry: IcalEntry, section: ListSection, index: Int) -> Unit
) {
    sections.forEach { section ->
        if (section.entries.isEmpty()) return@forEach

        val collapsed = section.header != null && section.key in collapsedGroups

        section.header?.let { header ->
            item(span = StaggeredGridItemSpan.FullLine, key = "header_${section.key}") {
                ListSectionHeader(
                    appPreferencesTag = section.key,
                    headerText = header.resolveText(),
                    headerIcon = if(header is ListSectionHeader.Res) header.iconDrawableRes else null,
                    isCollapsed = collapsed,
                    onToggleListGroupExpanded = onToggleGroup,
                    modifier = if (section.dimmed) Modifier.alpha(0.33f) else Modifier
                )
            }
        }

        if (!collapsed) {
            itemsIndexed(section.entries, key = { _, entry -> entry.uid }) { index, entry ->
                itemContent(entry, section, index)
            }
        }
    }
}
