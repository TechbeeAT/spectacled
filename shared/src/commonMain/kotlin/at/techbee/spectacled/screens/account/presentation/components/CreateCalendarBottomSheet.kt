package at.techbee.spectacled.screens.account.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.ProcessingState
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.presentation.components.BottomSheetWithMenu
import at.techbee.spectacled.screens.core.presentation.components.ColorSelectorElement
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.cancel
import spectacled.shared.generated.resources.create_folder
import spectacled.shared.generated.resources.description
import spectacled.shared.generated.resources.folder
import spectacled.shared.generated.resources.include_support_for_subtasks
import spectacled.shared.generated.resources.update_folder
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateOrUpdateCalendarBottomSheet(
    sheetState: SheetState,
    principal: Principal,
    homeCollection: HomeCollection,
    calendar: Calendar,
    processingState: ProcessingState,
    recentColors: List<Color>,
    onCreateOrUpdateCalendar: (Principal, HomeCollection, Calendar) -> Unit,
    onDismiss: () -> Unit,
    spectacledVariant: SpectacledVariant = koinInject()
    ) {

    var calendarName by rememberSaveable { mutableStateOf(calendar.displayName?:"") }
    var calendarDescription by rememberSaveable { mutableStateOf(calendar.calendarDescription?:"") }
    var color by remember { mutableStateOf(calendar.color) }
    var supportedCalendarComponents by remember {
        mutableStateOf(
            if(calendar.id == 0L && spectacledVariant in listOf(SpectacledVariant.JOURNALS, SpectacledVariant.NOTES))
                listOf(CalendarComponent.VJOURNAL, CalendarComponent.VTODO)
            else if(calendar.id == 0L)
                listOf(CalendarComponent.VTODO)
            else
                calendar.supportedComponents
        )
    }

    val focusRequester = remember { FocusRequester() }


    LaunchedEffect(Unit) {
        delay(300.milliseconds)
        focusRequester.requestFocus()
    }

    BottomSheetWithMenu(
        headline = if (calendar.id == 0L)
            stringResource(Res.string.create_folder)
        else
            stringResource(Res.string.update_folder),
        onDismiss = { onDismiss() },
        sheetState = sheetState,
        gesturesEnabled = false,
        showLoadingIndicator = processingState is ProcessingState.Processing,
        menuActionLeft = {
            TextButton(
                onClick = { onDismiss() },
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
        menuActionRight = {

            TextButton(
                onClick = {
                    onCreateOrUpdateCalendar(
                        principal,
                        homeCollection,
                        calendar.copy(
                            displayName = calendarName,
                            calendarDescription = calendarDescription,
                            color = if (color == Color.Unspecified) null else color,
                            supportedComponents = if(calendar.id == 0L) supportedCalendarComponents else calendar.supportedComponents
                        )
                    )
                },
                enabled = calendarName.isNotBlank()
            ) {
                Text(if (calendar.id == 0L)
                    stringResource(Res.string.create_folder)
                else
                    stringResource(Res.string.update_folder),
                )
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {

            val error = if (processingState is ProcessingState.Error) processingState.message else ""
            AnimatedVisibility(error.isNotEmpty()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value = calendarName,
                onValueChange = { calendarName = it },
                //leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) },
                label = { Text(stringResource(Res.string.folder)) },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester)
            )

            OutlinedTextField(
                value = calendarDescription,
                onValueChange = { calendarDescription = it },
                label = { Text(stringResource(Res.string.description)) },
                singleLine = true
            )

            // only visible for new folders
            if(calendar.id == 0L && spectacledVariant != SpectacledVariant.TASKS) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(stringResource(Res.string.include_support_for_subtasks))

                    Switch(
                        checked = supportedCalendarComponents.contains(CalendarComponent.VTODO),
                        onCheckedChange = {
                            supportedCalendarComponents = if (supportedCalendarComponents.contains(CalendarComponent.VTODO))
                                supportedCalendarComponents.minus(CalendarComponent.VTODO)
                            else
                                supportedCalendarComponents.plus(CalendarComponent.VTODO)
                        }
                    )
                }
            }



            ColorSelectorElement(
                recentColors = recentColors,
                preselectedColor = color,
                onColorChanged = { color = it },
                skipPartialSelection = false,
                modifier = Modifier
            )

            /*
            Button(
                onClick = {
                    onCreateOrUpdateCalendar(
                        principal,
                        calendar.copy(
                            displayName = calendarName,
                            calendarDescription = calendarDescription,
                            color = if (color == Color.Unspecified) null else color,
                        )
                    )
                },
                enabled = calendarName.isNotBlank(),
                modifier = Modifier.padding(16.dp)

            ) {
                Text(text = if (calendar.id == 0L)
                    stringResource(Res.string.create_folder)
                else
                    stringResource(Res.string.update_folder),)
            }

            AnimatedVisibility(processingState is ProcessingState.Processing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp)
                )
            }

            val error = if (processingState is ProcessingState.Error) processingState.message else ""
            AnimatedVisibility(error.isNotEmpty()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

             */
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun CreateOrUpdateCalendarBottomSheet_create_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            CreateOrUpdateCalendarBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                principal = Principal.getPrincipalForPreview(),
                homeCollection = HomeCollection.getHomeCollectionForPreview(),
                calendar = Calendar.getCalendarForPreview(),
                processingState = ProcessingState.Error("This is the error"),
                recentColors = emptyList(),
                onCreateOrUpdateCalendar = { _, _, _ -> },
                onDismiss = {},
                spectacledVariant = SpectacledVariant.JOURNALS
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun CreateOrUpdateCalendarBottomSheet_update_Preview() {
    AppTheme(spectacledVariant = SpectacledVariant.JOURNALS) {
        Scaffold {
            CreateOrUpdateCalendarBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                principal = Principal.getPrincipalForPreview(),
                homeCollection = HomeCollection.getHomeCollectionForPreview(),
                calendar = Calendar.getCalendarForPreview().copy(id = 1L),
                processingState = ProcessingState.Processing,
                recentColors = listOf(Color.Green, Color.Red),
                onCreateOrUpdateCalendar = { _, _, _ -> },
                onDismiss = {},
                spectacledVariant = SpectacledVariant.JOURNALS
            )
        }
    }

}
