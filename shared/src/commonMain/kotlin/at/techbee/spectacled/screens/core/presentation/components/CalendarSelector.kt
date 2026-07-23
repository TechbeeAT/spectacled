package at.techbee.spectacled.screens.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
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

                    if (calendarsGroups[principal]?.isEmpty() == true)
                        return@forEach

                    DropdownMenuItem(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                HorizontalDivider(modifier = Modifier.weight(0.5f))
                                Text(
                                    text = principal?.displayName ?: stringResource(Res.string.no_account_name),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontStyle = FontStyle.Italic,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(0.5f)
                                )
                            }
                        },
                        onClick = {},
                        enabled = false
                    )

                    calendarsGroups[principal]?.forEach { calendar ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = calendar.displayName ?: calendar.calendarDescription ?: stringResource(Res.string.unnamed_calendar),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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