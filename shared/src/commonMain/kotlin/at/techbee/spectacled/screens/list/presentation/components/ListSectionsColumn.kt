package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSection
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSectionHeader

/**
 * Renders the shared list skeleton (headers, collapse handling, trashbin dimming) for a [LazyColumn]
 * from a pre-built [sections] list. Journals month headers ([ListSectionHeader.Month]) render as
 * sticky [MonthHeader]s; every other header renders as a collapsible [ListSectionHeader]. Only the
 * per-entry content differs between screens, so it is supplied via [itemContent].
 *
 * @param itemContent renders one entry; receives its section and its index within that section so
 *   the caller can compute first/last-in-section styling from `section.entries`.
 */
fun LazyListScope.listSections(
    sections: List<ListSection>,
    collapsedGroups: Set<String>,
    onToggleGroup: (String) -> Unit,
    itemContent: @Composable LazyItemScope.(entry: IcalEntry, section: ListSection, index: Int) -> Unit
) {
    sections.forEach { section ->
        if (section.entries.isEmpty()) return@forEach

        val collapsed = section.header != null && section.key in collapsedGroups

        when (val header = section.header) {
            is ListSectionHeader.Month -> stickyHeader(key = "header_${section.key}") {
                MonthHeader(icsDateTime = header.icsDateTime, modifier = Modifier.padding(bottom = 8.dp))
            }
            null -> {} // header-less section (e.g. sorted by summary)
            else -> item(key = "header_${section.key}") {
                ListSectionHeader(
                    appPreferencesTag = section.key,
                    headerText = header.resolveText(),
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
