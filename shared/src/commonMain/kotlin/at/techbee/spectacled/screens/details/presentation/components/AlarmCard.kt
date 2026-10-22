package at.techbee.spectacled.screens.details.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.IcsDateTimeFormat
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.formatLocalized
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.theme.AppTheme
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.alarm
import spectacled.shared.generated.resources.alarm_fires_at
import spectacled.shared.generated.resources.alarm_fires_in_past
import spectacled.shared.generated.resources.alarm_invalid_alarm_trigger_time
import spectacled.shared.generated.resources.delete
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Composable
fun AlarmCard(
    alarm: Alarm,
    icalEntry: IcalEntry,
    allowEditing: Boolean,
    onAction: (DetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        onClick = { },
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        modifier = modifier
    ) {

        Column(modifier = Modifier.fillMaxWidth()) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {

                IconButton(
                    onClick = {},
                    enabled = false
                ) {
                    Icon(Icons.Outlined.Alarm, stringResource(Res.string.alarm))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            Res.string.alarm_fires_at,
                            alarm.triggerTime(icalEntry)?.let { IcsDateTime(it, isDateOnly = false, timeZone = TimeZone.currentSystemDefault()).formatLocalized(IcsDateTimeFormat.DATE_TIME)  } ?: stringResource(Res.string.alarm_invalid_alarm_trigger_time)
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (alarm.trigger is AlarmTrigger.Relative) {
                        Text(
                            text = relativeTriggerText(alarm.trigger),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if((alarm.triggerTime(icalEntry)?:Instant.DISTANT_PAST) < Clock.System.now())
                        Text(
                            text = stringResource(Res.string.alarm_fires_in_past),
                            style = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.error
                        )
                }


                if(allowEditing) {
                    IconButton(onClick = { onAction(DetailsAction.OnDeleteAlarm(alarm)) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(Res.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AlarmCard_Absolute_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            AlarmCard(
                alarm = Alarm(
                    trigger = AlarmTrigger.Absolute(Clock.System.now().plus(1.hours))
                ),
                icalEntry = IcalEntry.getSampleTask(),
                allowEditing = true,
                onAction = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun AlarmCard_Relative_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            AlarmCard(
                alarm = Alarm(
                    trigger = AlarmTrigger.Relative(IcsDuration())
                ),
                icalEntry = IcalEntry.getSampleTask(),
                allowEditing = true,
                onAction = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun AlarmCard_Relative_past_readonly_Preview() {

    AppTheme(spectacledVariant = SpectacledVariant.TASKS) {
        Scaffold {
            AlarmCard(
                alarm = Alarm(
                    trigger = AlarmTrigger.Relative(IcsDuration(isNegative = true, days = 1))
                ),
                icalEntry = IcalEntry.getSampleTask().copy(dtStart = IcsDateTime(Instant.DISTANT_PAST, false)),
                allowEditing = false,
                onAction = {},
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

