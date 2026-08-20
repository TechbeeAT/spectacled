package at.techbee.spectacled.screens.list.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSectionHeader
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.collapse
import spectacled.shared.generated.resources.expand
import spectacled.shared.generated.resources.ic_pin_rotated
import spectacled.shared.generated.resources.ic_trashbin

@Composable
fun ListSectionHeader(
    appPreferencesTag: String,
    headerText: String,
    headerIcon: DrawableResource? = null,
    isCollapsed: Boolean,
    onToggleListGroupExpanded: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = {
            onToggleListGroupExpanded(appPreferencesTag)
        },
        colors = ButtonDefaults.textButtonColors().copy(contentColor = LocalContentColor.current),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {

            headerIcon?.let { Icon(painterResource(it), null, tint = MaterialTheme.colorScheme.primary) }
            Text(
                text = headerText,
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Crossfade(isCollapsed) {
                if (it)
                    Icon(Icons.Outlined.ExpandLess, stringResource(Res.string.expand))
                else
                    Icon(Icons.Outlined.ExpandMore, stringResource(Res.string.collapse))
            }
        }
    }
}

/** Resolves a header model to the text shown in the collapsible header row; month headers render via
 *  [MonthHeader] instead and skip this. */
@Composable
internal fun ListSectionHeader.resolveText(): String = when (this) {
    is ListSectionHeader.Res ->
        (param?.let { stringResource(stringRes, it) } ?: stringResource(stringRes))
    is ListSectionHeader.Raw -> text
    // Month headers render via MonthHeader, not this path; provide a sensible fallback anyway.
    is ListSectionHeader.Month -> icsDateTime?.formatLocalized(IcsDateTimeFormat.FULL_MONTH_NAME) ?: ""
}


@Preview
@Composable
fun ListSectionHeaderTodayCollapsedPreview() {
    ListSectionHeader(
        appPreferencesTag = "today",
        headerText = "Today",
        isCollapsed = true,
        onToggleListGroupExpanded = {}
    )
}

@Preview
@Composable
fun ListSectionHeaderTomorrowExpandedPreview() {
    ListSectionHeader(
        appPreferencesTag = "date",
        headerText = IcsDateTime.now().formatLocalized(IcsDateTimeFormat.DATE),
        isCollapsed = false,
        onToggleListGroupExpanded = {}
    )
}

@Preview
@Composable
fun ListSectionHeaderTodayCollapsed_pinned_Preview() {
    ListSectionHeader(
        appPreferencesTag = "pinned",
        headerText = "Pinned",
        headerIcon = Res.drawable.ic_pin_rotated,
        isCollapsed = true,
        onToggleListGroupExpanded = {}
    )
}


@Preview
@Composable
fun ListSectionHeaderTodayCollapsed_trashbin_Preview() {
    ListSectionHeader(
        appPreferencesTag = "trashbin",
        headerText = "Trashbin",
        headerIcon = Res.drawable.ic_trashbin,
        isCollapsed = true,
        onToggleListGroupExpanded = {}
    )
}