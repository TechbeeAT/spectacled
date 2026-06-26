package at.techbee.spectacled.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.presentation.components.CalendarSelector
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.widget.SpectacledWidget.Companion.CALENDAR_ID_KEY
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.done

class SpectacledWidgetConfigActivity : ComponentActivity(), KoinComponent {

    private val calendarRepository: CalendarRepository by inject()
    private val spectacledVariant: SpectacledVariant by inject()
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)

            val prefs by produceState(null as Preferences?) {
                value = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            }

            val calendars by produceState(emptyList()) {
                calendarRepository.getAllCalendarsFlow().collect { value = it }
            }
            val homeCollections by produceState(emptyList()) {
                calendarRepository.getAllHomeCollectionsFlow().collect { value = it }
            }
            val principals by produceState(emptyList()) {
                calendarRepository.getAllPrincipalsFlow().collect { value = it }
            }

            WidgetConfigContent(
                prefs,
                calendars,
                homeCollections,
                principals,
                spectacledVariant,
                onConfirm = {
                    scope.launch {
                        onCalendarSelected(it, glanceId)
                    }
                }
            )
        }
    }

    private suspend fun onCalendarSelected(calendarId: Long, glanceId: GlanceId) {
        try {
            updateAppWidgetState(this@SpectacledWidgetConfigActivity, glanceId) { prefs ->
                prefs[longPreferencesKey(CALENDAR_ID_KEY)] = calendarId
            }
            SpectacledWidget().update(this@SpectacledWidgetConfigActivity, glanceId)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigContent(
    prefs: Preferences?,
    calendars: List<Calendar>,
    homeCollections: List<HomeCollection>,
    principals: List<Principal>,
    spectacledVariant: SpectacledVariant,
    onConfirm: (Long) -> Unit
) {


    var selectedCalendarId by remember {
        mutableStateOf(null as Long?)
    }

    LaunchedEffect(prefs, calendars) {
        if (selectedCalendarId == null) {
            val savedId = prefs?.get(longPreferencesKey(CALENDAR_ID_KEY))
            if (savedId != null) {
                selectedCalendarId = savedId
            }
        }
    }

    AppTheme(spectacledVariant = spectacledVariant) {
        Scaffold (
            topBar = {
                TopAppBar(
                    title = { },
                    actions = {
                        TextButton(
                            onClick = {
                                selectedCalendarId?.let { onConfirm(it) }
                            },
                            enabled = selectedCalendarId != null,
                        ) {
                            Text(stringResource(Res.string.done))
                        }
                    }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(paddingValues).padding(16.dp)) {

                Text(
                    text = "Widget configuration",
                    style = MaterialTheme.typography.headlineSmall
                )

                CalendarSelector(
                    principals = principals,
                    homeCollections = homeCollections,
                    calendars = calendars,
                    selectedCalendarId = selectedCalendarId,
                    onCalendarIdSelected = { selectedCalendarId = it },
                    modifier = Modifier.fillMaxWidth().padding (16.dp)
                )

/*
                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { selectedCalendarId?.let { onConfirm(it) } },
                    enabled = selectedCalendarId != null,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(stringResource(Res.string.done))
                }
 */
            }
        }
    }
}

@Preview
@Composable
private fun WidgetConfigContent_Preview() {
    WidgetConfigContent(
        prefs = null,
        calendars = listOf(Calendar.getCalendarForPreview()),
        homeCollections = listOf(HomeCollection.getHomeCollectionForPreview()),
        principals = listOf(Principal.getPrincipalForPreview()),
        spectacledVariant = SpectacledVariant.JOURNALS,
        onConfirm = {}
    )
}