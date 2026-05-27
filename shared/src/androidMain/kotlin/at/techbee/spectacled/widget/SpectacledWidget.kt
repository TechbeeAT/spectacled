package at.techbee.spectacled.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.getAndroidLogoResId
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SpectacledWidget : GlanceAppWidget(), KoinComponent {

    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val spectacledVariant: SpectacledVariant by inject()

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
        val appName = getString(spectacledVariant.appNameStringRes)

        provideContent {
            val prefs = currentState<Preferences>()
            val calendarId = prefs[longPreferencesKey(CALENDAR_ID_KEY)]

            val entries = if (calendarId != null) {
                database
                    .icalentry_dtoQueries
                    .getIcalEntriesByCalendar(calendarId)
                    .executeAsList()
                    .map { it.toDomain() }
                    .filter { !it.syncState.isDeletedState() }
                    .sortedByDescending { it.dtStart?.instant?.toEpochMilliseconds() ?: it.created.instant.toEpochMilliseconds() }
            } else {
                emptyList()
            }

            GlanceTheme {
                SpectacledWidgetContent(entries, appName, calendarId == null)
            }
        }
    }

    @Composable
    fun SpectacledWidgetContent(entries: List<IcalEntry>, title: String, isNotConfigured: Boolean) {

        Scaffold(
            titleBar = {
                TitleBar(
                    startIcon = ImageProvider(spectacledVariant.getAndroidLogoResId()),
                    iconColor = GlanceTheme.colors.primary,
                    title = title
                )
            }
        ) {
            Column(
                modifier = GlanceModifier
                    .padding(bottom = 2.dp, start = 2.dp, end = 2.dp, top = 0.dp)
                    .cornerRadius(8.dp)
                    .fillMaxSize()
                ,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {

                if (isNotConfigured) {
                    Text(text = "Please select a calendar in widget settings")
                } else if (entries.isEmpty()) {
                    Text(text = "No entries found")
                } else {
                    LazyColumn(
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .fillMaxSize()
                    ) {
                        itemsIndexed(entries) { index, entry ->

                            Column {
                                JournalEntryItem(entry)

                                if (index != entries.lastIndex) {
                                    Box(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .padding(horizontal = 24.dp, vertical = 2.dp)
                                            .background(GlanceTheme.colors.widgetBackground)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun JournalEntryItem(entry: IcalEntry) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 4.dp)
                .background(GlanceTheme.colors.surface)
        ) {

            Text(
                text = entry.summary ?: "No Title",
                style = TextStyle(fontWeight = FontWeight.Bold)
            )
            entry.description?.let {
                Text(
                    text = it,
                    maxLines = 2
                )
            }
        }
    }

    companion object {
        const val CALENDAR_ID_KEY = "calendar_id"
    }
}
