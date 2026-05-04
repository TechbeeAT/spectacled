package at.techbee.spectacled.screens.details.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.PlatformInstantFormatter
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.presentation.MarkdownVisualTransformation
import at.techbee.spectacled.theme.getContentColorForColoredSurfaces
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_time
import spectacled.shared.generated.resources.description
import spectacled.shared.generated.resources.no_timezone
import spectacled.shared.generated.resources.summary


@Composable
fun DetailsScreen(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var summaryIsFocused by rememberSaveable { mutableStateOf(false) }
    var descriptionIsFocused by rememberSaveable { mutableStateOf(false) }


    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {

        state.icalEntry.dtStart?.let { dtStart ->

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                AssistChip(
                    onClick = { onAction(DetailsAction.OnShowDatePickerBottomSheet(true)) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                    label = { Text(PlatformInstantFormatter(dtStart).formatLocalizedDate()) },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary)
                )

                AssistChip(
                    onClick = { onAction(DetailsAction.OnShowTimePickerBottomSheet(true)) },
                    leadingIcon = {
                        if (!dtStart.isDateOnly)
                            Icon(Icons.Outlined.Schedule, null)
                    },
                    label = {
                        if(dtStart.isDateOnly)
                            Icon(
                                imageVector = Icons.Outlined.MoreTime,
                                contentDescription = stringResource(Res.string.add_time),
                                tint = state.icalEntry.color ?: MaterialTheme.colorScheme.primary
                            )
                        else
                            Text(PlatformInstantFormatter(dtStart).formatLocalizedTime())
                    },
                    colors = AssistChipDefaults.assistChipColors()
                        .copy(leadingIconContentColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary)
                )

                AnimatedVisibility(!dtStart.isDateOnly) {
                    AssistChip(
                        onClick = { onAction(DetailsAction.OnShowTimezonePickerBottomSheet(true)) },
                        leadingIcon = {
                            if(dtStart.timeZone != null)
                                Icon(Icons.Outlined.Language, null)
                        },
                        label = {
                            if (dtStart.timeZone == null) {
                                Icon(
                                    imageVector = Icons.Outlined.Language,
                                    contentDescription = stringResource(Res.string.no_timezone),
                                    tint = state.icalEntry.color ?: MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Column {
                                    Text(dtStart.timeZone.id)
                                    Text(
                                        text = PlatformInstantFormatter(dtStart.copy(timeZone = TimeZone.currentSystemDefault())).formatLocalizedDateTime() +
                                                " (${TimeZone.currentSystemDefault().id})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }

                            }
                        },
                        colors = AssistChipDefaults.assistChipColors()
                            .copy(leadingIconContentColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(state.icalEntry.categories.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                state.icalEntry.categories.sorted().forEach { category ->

                    Badge(
                        containerColor = state.icalEntry.color ?: MaterialTheme.colorScheme.primary,
                        contentColor = state.icalEntry.color?.let { getContentColorForColoredSurfaces(state.icalEntry.color) }
                            ?: MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Text(
                            text = category,
                            modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        BasicTextField(
            value = if (!summaryIsFocused && state.icalEntry.summary.isNullOrEmpty()) stringResource(Res.string.summary) else state.icalEntry.summary
                ?: "",
            onValueChange = {
                onAction(DetailsAction.OnUpdateSummary(it))
            },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = if (!summaryIsFocused && state.icalEntry.summary.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            ),
            enabled = state.allowEditing(),
            visualTransformation = MarkdownVisualTransformation(LocalContentColor.current),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .onFocusChanged {
                    summaryIsFocused = it.isFocused
                }
        )

        BasicTextField(
            value = if (!descriptionIsFocused && state.icalEntry.description.isNullOrEmpty()) stringResource(Res.string.description) else state.icalEntry.description
                ?: "",
            onValueChange = {
                onAction(DetailsAction.OnUpdateDescription(it))
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (!descriptionIsFocused && state.icalEntry.description.isNullOrEmpty()) LocalContentColor.current.copy(alpha = 0.5f) else LocalContentColor.current
            ),
            enabled = state.allowEditing(),
            visualTransformation = MarkdownVisualTransformation(LocalContentColor.current),
            modifier = Modifier
                .heightIn(min = 200.dp)
                .fillMaxWidth()
                .fillMaxHeight(1f)
                .onFocusChanged {
                    descriptionIsFocused = it.isFocused
                }
        )
    }



}


@Preview
@Composable
private fun ListScreen_Preview() {
    DetailsScreen(
        state = DetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry(),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun ListScreen_with_dtstart_Preview() {
    DetailsScreen(
        state = DetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry().copy(dtStart = IcsDateTime.now()),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {}
    )
}

@Preview
@Composable
private fun ListScreen_with_dtstart_and_timezone_Preview() {
    DetailsScreen(
        state = DetailsState(
            icalEntry = IcalEntry.getSampleIcalEntry().copy(dtStart = IcsDateTime.now().copy(timeZone = TimeZone.of("Europe/Vienna"))),
            originalIcalEntry = IcalEntry.getSampleIcalEntry()
        ),
        onAction = {},
        modifier = Modifier.fillMaxHeight()
    )
}


