package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_account_name
import spectacled.shared.generated.resources.selected_calendar
import spectacled.shared.generated.resources.unnamed_calendar

@Composable
fun CalendarSelector(
    principals: List<Principal>,
    homeCollections: List<HomeCollection>,
    calendars: List<Calendar>,
    selectedCalendarId: Long?,
    onCalendarIdSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {

    var calendarsExpanded by remember { mutableStateOf(false) }

    ElevatedAssistChip(
        onClick = { calendarsExpanded = !calendarsExpanded },
        label = {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = stringResource(Res.string.selected_calendar),
                    style = MaterialTheme.typography.labelSmall
                )
                calendars.find { calendar -> calendar.id == selectedCalendarId }?.let {
                    Text(it.displayName ?: it.url.toString())
                } ?: Text("-")
            }


            DropdownMenu(
                expanded = calendarsExpanded,
                onDismissRequest = { calendarsExpanded = false }
            ) {

                val calendarsGroups = calendars.groupBy { calendar ->
                    homeCollections.find { homeCollection -> homeCollection.id == calendar.homeCollectionId }.let {
                            homeCollection -> principals.find { principal -> homeCollection?.principalId == principal.id }
                    }
                }
                calendarsGroups.keys.forEach { principal ->

                    calendarsGroups[principal]?.forEach { calendar ->
                        DropdownMenuItem(
                            text = {
                                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text(
                                        text = principal?.displayName ?: stringResource(Res.string.no_account_name),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontStyle = FontStyle.Italic
                                    )
                                    Text(text = calendar.displayName ?: stringResource(Res.string.unnamed_calendar))
                                    calendar.calendarDescription?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                onCalendarIdSelected(calendar.id)
                                calendarsExpanded = false
                            }
                        )

                    }

                }
            }

        },
        trailingIcon = {
            Icon(Icons.Outlined.ArrowDropDown, null)
        },
        modifier = modifier
    )
}

@Preview
@Composable
private fun CalendarSelector_Preview() {
    AppTheme(
        spectacledVariant = SpectacledVariant.JOURNALS
    ) {
        CalendarSelector(
            calendars = listOf(Calendar.getCalendarForPreview()),
            homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
            principals = listOf(Principal.getPrincipalForPreview()),
            selectedCalendarId = null,
            onCalendarIdSelected = {},
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        )
    }
}