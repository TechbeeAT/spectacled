package at.techbee.spectacled.screens.note.presentation.notedetails.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.presentation.BottomSheetWithMenu
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.no_timezone
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeZonePickerBottomSheet(
    icsDateTime: IcsDateTime,
    sheetState: SheetState,
    onTimeZoneUpdated: (IcsDateTime) -> Unit,
    onDismiss: () -> Unit
) {

    val lazyListState = rememberLazyListState()
    var timeZoneSearchQuery by mutableStateOf("")
    var filteredTimeZones by mutableStateOf(TimeZone.availableZoneIds.toList().sorted())

    LaunchedEffect(icsDateTime.timeZone) {
        if(filteredTimeZones.indexOf(icsDateTime.timeZone?.id ?: TimeZone.currentSystemDefault().id) > -1)
            lazyListState.scrollToItem(filteredTimeZones.indexOf(icsDateTime.timeZone?.id ?: TimeZone.currentSystemDefault().id))
    }

    BottomSheetWithMenu(
        sheetState = sheetState,
        onDismiss = { onDismiss() },
        menuAction = {
            TextButton(
                onClick = {
                    onTimeZoneUpdated(icsDateTime.withZone(null))
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.no_timezone))
            }
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            TextField(
                value = timeZoneSearchQuery,
                onValueChange = {
                    timeZoneSearchQuery = it
                    filteredTimeZones = if(it.isBlank())
                        TimeZone.availableZoneIds.toList().sorted()
                    else
                        TimeZone.availableZoneIds.filter { zoneId -> zoneId.uppercase().contains(it.uppercase()) }.sorted()
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                state = lazyListState,
                modifier = Modifier.padding(16.dp)
            ) {

                items(filteredTimeZones) { filteredTimeZone ->
                    val zone = TimeZone.of(filteredTimeZone)

                    FilterChip(
                        selected = zone == icsDateTime.timeZone,
                        onClick = { onTimeZoneUpdated(icsDateTime.withZone(zone)) },
                        label = { Text(filteredTimeZone) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun TimeZonePickerBottomSheet_Preview() {
    TimeZonePickerBottomSheet(
        icsDateTime = IcsDateTime.now().copy(timeZone = TimeZone.of("Europe/Vienna")),
        sheetState = rememberModalBottomSheetState(),
        onTimeZoneUpdated = { },
        onDismiss = {}
    )
}
