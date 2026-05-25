package at.techbee.spectacled.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SpectacledWidget : GlanceAppWidget(), KoinComponent {

    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val userAppPreferencesStore: PlatformUserAppPreferencesStore by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)
        val calendarId = userAppPreferencesStore.lastUsedCalendarId
        val entries = database
            .icalentry_dtoQueries
            .getIcalEntriesByCalendar(calendarId!!)
            .executeAsList()
            .map { it.toDomain() }
            .filter { !it.syncState.isDeletedState() }
            .sortedByDescending { it.dtStart?.instant?.toEpochMilliseconds() ?: it.created.instant.toEpochMilliseconds() }


        provideContent {

            GlanceTheme {
                SpectacledWidgetContent(entries)
            }
        }
    }

    @Composable
    fun SpectacledWidgetContent(entries: List<IcalEntry>) {

        Scaffold(
            titleBar = {
                Text(
                    text = "Journal Entries",
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(vertical = 8.dp, horizontal = 16.dp)
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

                if (entries.isEmpty()) {
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
}
