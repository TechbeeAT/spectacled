package at.techbee.spectacled.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.core.getAndroidLogoResId
import at.techbee.spectacled.shared.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SpectacledWidget : GlanceAppWidget(), KoinComponent {

    private val spectacledVariant: SpectacledVariant by inject()
    private val calendarRepository: CalendarRepository by inject()
    private val icalEntryRepository: IcalEntryRepository by inject()

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {


        provideContent {
            val prefs = currentState<Preferences>()
            val calendarId = prefs[longPreferencesKey(CALENDAR_ID_KEY)]


            val calendar by produceState<Calendar?>(initialValue = null, key1 = calendarId) {
                value = calendarId?.let { calendarRepository.getCalendarById(it) }
            }

            val entries by remember(calendarId) {
                if (calendarId != null) {
                    icalEntryRepository
                        .getIcalEntriesByCalendarFlow(calendarId)
                        .map { list ->
                            list.filter { !it.syncState.isDeletedState() }
                                .sortedByDescending { it.dtStart?.instant?.toEpochMilliseconds() ?: it.created.instant.toEpochMilliseconds() }
                                .groupBy { it.parentUid }
                        }
                } else {
                    flowOf(emptyMap())
                }
            }.collectAsState(emptyMap())

            GlanceTheme {
                SpectacledWidgetContent(entries, calendar)
            }
        }
    }

    @Composable
    fun SpectacledWidgetContent(entries: Map<String?, List<IcalEntry>>, calendar: Calendar?) {

        val context = LocalContext.current

        Scaffold(
            titleBar = {
                TitleBar(
                    startIcon = ImageProvider(spectacledVariant.getAndroidLogoResId()),
                    iconColor = GlanceTheme.colors.primary,
                    title = calendar?.displayName ?: calendar?.url?.toString() ?: "",
                    actions = {
                        if (calendar != null) {
                            SquareIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_add),
                                contentDescription = context.getString(R.string.widget_new_entry),
                                onClick = actionStartActivity(
                                    getLaunchIntent(context, calendar.id, 0L)
                                ),
                                backgroundColor = GlanceTheme.colors.widgetBackground,
                                contentColor = GlanceTheme.colors.onBackground
                            )

                            SquareIconButton(
                                imageProvider = ImageProvider(R.drawable.ic_open_in_new),
                                contentDescription = context.getString(R.string.widget_open_calendar),
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

                val mainEntries = entries[null] ?: emptyList()    // no subtasks

                if (calendar == null) {
                    WidgetEmptyState(context.getString(R.string.widget_select_calendar), true)
                } else if (mainEntries.isEmpty()) {
                    WidgetEmptyState(context.getString(R.string.widget_no_entries), false)
                } else {
                    LazyColumn(
                        modifier = GlanceModifier
                            .cornerRadius(8.dp)
                            .fillMaxSize()
                    ) {
                        itemsIndexed(mainEntries) { index, entry ->

                            Column(modifier = GlanceModifier.fillMaxWidth()) {

                                IcalEntryItem(
                                    entry = entry,
                                    allowEditing = calendar.canWriteContent() && !entry.syncState.isDeletedState() && !entry.isRecurring(),
                                    modifier = GlanceModifier.clickable(
                                        onClick = actionStartActivity(
                                            getLaunchIntent(context, entry.calendarId, entry.id)
                                        )
                                    )
                                )

                                entries[entry.uid]?.forEach { subEntry ->
                                    IcalEntryItem(
                                        entry = subEntry,
                                        showSubtaskIcon = true,
                                        allowEditing = calendar.canWriteContent() && !subEntry.syncState.isDeletedState() && !subEntry.isRecurring(),
                                        modifier = GlanceModifier
                                            .clickable(
                                                onClick = actionStartActivity(
                                                    getLaunchIntent(context, subEntry.calendarId, subEntry.id)
                                                )
                                            )
                                    )
                                }

                                if (index != mainEntries.lastIndex) {
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
    private fun WidgetEmptyState(message: String, showConfigureButton: Boolean) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier.padding(16.dp)) {
            Text(
                text = message,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                ),
                modifier = GlanceModifier.padding(16.dp)
            )

            if(showConfigureButton) {
                Button(
                    text = LocalContext.current.getString(R.string.widget_configure),
                    modifier = GlanceModifier.padding(16.dp),
                    onClick = actionRunCallback<OpenConfigAction>()
                )
            }
        }
    }

    @Composable
    private fun IcalEntryItem(
        entry: IcalEntry,
        allowEditing: Boolean,
        showSubtaskIcon: Boolean = false,
        modifier: GlanceModifier = GlanceModifier
    ) {

        Row(modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
            .background(GlanceTheme.colors.surface)
            .then(modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if(showSubtaskIcon)
                Image(
                    provider = ImageProvider(R.drawable.ic_sub),
                    contentDescription = LocalContext.current.getString(R.string.widget_subtask),
                    modifier = GlanceModifier.size(16.dp).padding(end = 4.dp),
                    contentScale = ContentScale.FillBounds,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onBackground)
                )


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
                if(allowEditing)
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
        val icalEntryRepository: IcalEntryRepository by inject()

        val entryId = parameters[EntryIdKey] ?: return
        val isChecked = parameters[IsCheckedKey] ?: return

        // Recurring entries are read-only (this app has no recurrence support), so ignore the
        // toggle even if a stale widget still shows an enabled checkbox for one.
        if (icalEntryRepository.getIcalEntryById(entryId)?.isRecurring() == true)
            return

        val newPercent = if (isChecked) 100L else 0L
        val newStatus = if (isChecked) Status.COMPLETED else Status.NEEDS_ACTION

        icalEntryRepository.updateProgress(
            id = entryId,
            percentComplete = newPercent,
            status = newStatus,
            lastModified = IcsDateTime.now(),
            syncState = SyncState.LOCAL_MODIFIED
        )

        // Force a UI update by changing a preference.
        // This ensures Glance recognizes a state change.
        /*
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[longPreferencesKey("last_widget_update")] = System.currentTimeMillis()
            }
        }
         */

        SpectacledWidget().update(context, glanceId)
    }

    companion object {
        val EntryIdKey = ActionParameters.Key<Long>("entryId")
        val IsCheckedKey = ActionParameters.Key<Boolean>("isChecked")
    }
}


class OpenConfigAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, SpectacledWidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, GlanceAppWidgetManager(context).getAppWidgetId(glanceId))
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
