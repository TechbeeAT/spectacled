package at.techbee.spectacled.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.getAndroidLogoResId
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SpectacledWidget : GlanceAppWidget(), KoinComponent {

    private val databaseDriverFactory: DatabaseDriverFactory by inject()
    private val spectacledVariant: SpectacledVariant by inject()

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {

        val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

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

            val calendar = if (calendarId != null) {
                database
                    .calendar_dtoQueries
                    .getCalendarById(calendarId)
                    .executeAsOneOrNull()?.toDomain()
            } else null

            GlanceTheme {
                SpectacledWidgetContent(entries, calendar)
            }
        }
    }

    @Composable
    fun SpectacledWidgetContent(entries: List<IcalEntry>, calendar: Calendar?) {

        val context = LocalContext.current

        Scaffold(
            titleBar = {
                TitleBar(
                    startIcon = ImageProvider(spectacledVariant.getAndroidLogoResId()),
                    iconColor = GlanceTheme.colors.primary,
                    title = calendar?.displayName ?:calendar?.url?.toString() ?: "",
                    actions = {
                        if (calendar != null) {
                            SquareIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_add),
                                contentDescription = "New entry",
                                onClick = actionStartActivity(
                                    getLaunchIntent(context, calendar.id, 0L)
                                ),
                                backgroundColor = GlanceTheme.colors.widgetBackground,
                                contentColor = GlanceTheme.colors.onBackground
                            )

                            SquareIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_open_in_new),
                                contentDescription = "Open calendar",
                                onClick = actionStartActivity(
                                    getLaunchIntent(context, calendar.id)
                                ),
                                backgroundColor = GlanceTheme.colors.widgetBackground,
                                contentColor = GlanceTheme.colors.onBackground
                            )
                        }


                    }
                )
            },
        ) {
            Box(
                modifier = GlanceModifier
                    .cornerRadius(8.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                if (calendar == null) {
                    WidgetEmptyState("Please select a calendar in widget settings")
                } else if (entries.isEmpty()) {
                    WidgetEmptyState("No entries found")
                } else {
                    LazyColumn(
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .fillMaxSize()
                    ) {
                        itemsIndexed(entries) { index, entry ->

                            Column(modifier = GlanceModifier.clickable(
                                onClick = actionStartActivity(
                                    getLaunchIntent(context, calendar.id, entry.id)
                                )
                            )) {
                                IcalEntryItem(entry)

                                if (index != entries.lastIndex) {
                                    Box(
                                        modifier = GlanceModifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .padding(horizontal = 16.dp)
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
    private fun WidgetEmptyState(message: String) {
        Text(
            text = message,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = GlanceModifier.padding(16.dp)
        )
    }

    @Composable
    private fun IcalEntryItem(entry: IcalEntry) {

        Row(modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .background(GlanceTheme.colors.surface)) {


            Column(modifier = GlanceModifier.defaultWeight()) {

                Text(
                    text = entry.summary ?: "No Title",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
                entry.description?.let {
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = it.replace("\n", " "),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 2
                    )
                }
            }

            if(entry.isTask()) {
                CheckBox(
                    checked = entry.percentComplete == 100L,
                    onCheckedChange = actionRunCallback<ToggleTaskAction>(
                        actionParametersOf(
                            ToggleTaskAction.EntryIdKey to entry.id,
                            ToggleTaskAction.IsCheckedKey to (entry.percentComplete != 100L)
                        )
                    )
                )
            }
        }
    }

    private fun getLaunchIntent(context: Context, calendarId: Long?, entryId: Long? = null): Intent {
        return context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            calendarId?.let { putExtra(CALENDAR_ID_KEY, it) }
            entryId?.let { putExtra(ICAL_ENTRY_ID_KEY, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: Intent()
    }

    companion object {
        const val CALENDAR_ID_KEY = "calendar_id"
        const val ICAL_ENTRY_ID_KEY = "ical_entry_id"

        fun updateAll(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(SpectacledWidget::class.java)
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[longPreferencesKey("last_widget_update")] = System.currentTimeMillis()
                        }
                    }
                }
                SpectacledWidget().updateAll(context)
            }
        }
    }
}

class ToggleTaskAction : ActionCallback, KoinComponent {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val entryId = parameters[EntryIdKey] ?: return
        val isChecked = parameters[IsCheckedKey] ?: return

        val databaseDriverFactory: DatabaseDriverFactory by inject()
        val database = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

        val newPercent = if (isChecked) 100L else 0L
        val newStatus = if (isChecked) Status.COMPLETED else Status.NEEDS_ACTION

        database.icalentry_dtoQueries.updateProgress(
            newPercent = newPercent,
            newStatus = newStatus.name,
            lastModified = formatIcsDateTime(IcsDateTime.now())?.first,
            syncState = SyncState.LOCAL_MODIFIED.name,
            id = entryId
        )

        // Trigger a state update to force Glance to re-run provideGlance
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[longPreferencesKey("last_widget_update")] = System.currentTimeMillis()
            }
        }

        SpectacledWidget().update(context, glanceId)
    }

    companion object {
        val EntryIdKey = ActionParameters.Key<Long>("entryId")
        val IsCheckedKey = ActionParameters.Key<Boolean>("isChecked")
    }
}
