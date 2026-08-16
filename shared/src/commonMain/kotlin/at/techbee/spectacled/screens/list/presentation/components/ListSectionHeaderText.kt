package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.runtime.Composable
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSectionHeader
import org.jetbrains.compose.resources.stringResource

/** Resolves a [ListSectionHeader] to its displayable text (used by the flat [ListGroupHeader] path). */
@Composable
internal fun ListSectionHeader.resolveText(): String = when (this) {
    is ListSectionHeader.Res ->
        (param?.let { stringResource(stringRes, it) } ?: stringResource(stringRes)) + (suffix ?: "")
    is ListSectionHeader.Raw -> text
    // Month headers render via MonthHeader, not this path; provide a sensible fallback anyway.
    is ListSectionHeader.Month -> icsDateTime?.formatLocalized(IcsDateTimeFormat.FULL_MONTH_NAME) ?: ""
}
