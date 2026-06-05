package at.techbee.spectacled.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.theme.AppTheme
import at.techbee.spectacled.widget.SpectacledWidget.Companion.CALENDAR_ID_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.done

class SpectacledWidgetConfigActivity : ComponentActivity(), KoinComponent {

    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val spectacledVariant: SpectacledVariant by inject()
    private val userAppPreferencesStore: PlatformUserAppPreferencesStore by inject()
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
            val context = LocalContext.current
            val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
            val scope = rememberCoroutineScope()

            val prefs by produceState(null as Preferences?) {
                value = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
            }

            val calendarsForWidgetConfig by produceState(emptyList()) {
                val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
                database.calendar_dtoQueries.calendarsForWidgetConfig()
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .collect { value = it }
            }

            var selectedCalendarId by remember {
                mutableStateOf(null as Long?)
            }

            LaunchedEffect(prefs, calendarsForWidgetConfig) {
                if (selectedCalendarId == null) {
                    val savedId = prefs?.get(longPreferencesKey(CALENDAR_ID_KEY))
                    if (savedId != null) {
                        selectedCalendarId = savedId
                    }
                }
            }
            var calendarsExpanded by remember { mutableStateOf(false) }

            AppTheme(spectacledVariant = spectacledVariant) {
                Scaffold (
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

                        ElevatedAssistChip(
                            onClick = { calendarsExpanded = !calendarsExpanded },
                            label = {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Selected calendar",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    calendarsForWidgetConfig
                                        .find { calendar -> calendar.id == selectedCalendarId }?.let {
                                            Text(it.displayName ?: it.url)
                                        }
                                        ?: Text("-")
                                }


                                DropdownMenu(
                                    expanded = calendarsExpanded,
                                    onDismissRequest = { calendarsExpanded = false }
                                ) {

                                    val calendarsGroups = calendarsForWidgetConfig.groupBy { it.principalDisplayName }
                                    calendarsGroups.keys.forEach { principal ->

                                        Text(
                                            text = principal?:"No account name",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        calendarsGroups[principal]!!.forEach { calendar ->
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(text = calendar.displayName ?: "Unnamed Calendar")
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
                                                    selectedCalendarId = calendar.id
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
                            modifier = Modifier.fillMaxWidth().padding (16.dp)
                        )


                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                selectedCalendarId?.let {
                                    scope.launch {
                                        onCalendarSelected(it, glanceId)
                                    }
                                }
                            },
                            enabled = selectedCalendarId != null,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(stringResource(Res.string.done))
                        }
                    }
                }
            }
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
