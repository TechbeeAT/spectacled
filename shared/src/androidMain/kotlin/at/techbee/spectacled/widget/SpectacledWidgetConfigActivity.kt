package at.techbee.spectacled.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SpectacledWidgetConfigActivity : ComponentActivity(), KoinComponent {

    private val databaseDriverFactory: DatabaseDriverFactory by inject()
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
            val calendars by produceState(emptyList()) {
                val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
                database.calendar_dtoQueries.getAllCalendars()
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .collect { calendarDto -> value = calendarDto.map { it.toDomain() } }
            }

            AppTheme(spectacledVariant = spectacledVariant) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select a Calendar",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LazyColumn {
                            items(calendars) { calendar ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCalendarSelected(calendar.id)
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(text = calendar.displayName ?: "Unnamed Calendar")
                                    calendar.calendarDescription?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun onCalendarSelected(calendarId: Long) {
        val glanceAppWidgetManager = GlanceAppWidgetManager(this)
        MainScope().launch {
            val glanceId = glanceAppWidgetManager.getGlanceIdBy(appWidgetId)
            updateAppWidgetState(this@SpectacledWidgetConfigActivity, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[longPreferencesKey(SpectacledWidget.CALENDAR_ID_KEY)] = calendarId
                }
            }
            SpectacledWidget().update(this@SpectacledWidgetConfigActivity, glanceId)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }
}
