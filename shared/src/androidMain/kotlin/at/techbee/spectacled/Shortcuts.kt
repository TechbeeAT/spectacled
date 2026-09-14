package at.techbee.spectacled

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import at.techbee.spectacled.screens.core.data.LAST_USED_CALENDAR_ID
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.shared.R
import at.techbee.spectacled.widget.SpectacledWidget.Companion.CALENDAR_ID_KEY
import at.techbee.spectacled.widget.SpectacledWidget.Companion.ICAL_ENTRY_ID_KEY
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * The launcher's dynamic shortcuts: one that always adds an entry, and — once there is a last
 * used calendar — one that adds an entry straight into it.
 *
 * The second one depends on state that changes while the app runs (which calendar was opened
 * last, and how that calendar is named), so [observe] keeps collecting instead of taking a
 * snapshot at startup. A one-shot call from `onCreate` would not be enough: MainActivity is
 * `singleTask`, so returning to the app normally goes through `onNewIntent` and the shortcuts
 * would keep showing whatever was current when the process was started.
 *
 * Dependencies come from Koin through [KoinComponent], the same way [at.techbee.spectacled.widget.SpectacledWidget]
 * resolves its own, rather than through a global `KoinPlatform.getKoin()` lookup.
 */
object Shortcuts : KoinComponent {

    private const val SHORTCUT_ID_NEW_ENTRY = "new_entry"
    private const val SHORTCUT_ID_NEW_ENTRY_IN_CALENDAR_PREFIX = "new_entry_in_calendar_"

    private val userAppPreferencesStore: UserAppPreferencesStore by inject()
    private val calendarRepository: CalendarRepository by inject()

    /**
     * Publishes the shortcuts and re-publishes them on every change, until the calling scope is
     * cancelled. Collect it only while the activity is started — `setDynamicShortcuts` is rate
     * limited for background apps, so an update that arrives (e.g. through a sync) while the app
     * is not visible can be dropped silently:
     *
     * ```
     * lifecycleScope.launch {
     *     repeatOnLifecycle(Lifecycle.State.STARTED) {
     *         Shortcuts.observe(this@MainActivity, SpectacledVariant.JOURNALS)
     *     }
     * }
     * ```
     */
    suspend fun observe(context: Context, spectacledVariant: SpectacledVariant) {
        val appContext = context.applicationContext

        combine(
            userAppPreferencesStore.loadAsFlow(LAST_USED_CALENDAR_ID).map { it?.toLongOrNull() },
            calendarRepository.getAllCalendarsFlow()
        ) { lastUsedCalendarId, calendars ->
            // Resolved against the full list rather than with getCalendarById so that renaming or
            // deleting the calendar updates the shortcut too, not just picking a different one.
            calendars.firstOrNull { it.id == lastUsedCalendarId }
        }
            // Only these three fields reach the shortcut; mapping to them first keeps every sync
            // from re-publishing over churn on the rest of the Calendar (ctag, syncToken, ...).
            .map { calendar ->
                calendar?.let { LastUsedCalendar(it.id, it.shortcutLabel(), it.color?.toArgb()) }
            }
            .distinctUntilChanged()
            .collect { lastUsedCalendar -> publish(appContext, spectacledVariant, lastUsedCalendar) }
    }

    private suspend fun publish(
        context: Context,
        spectacledVariant: SpectacledVariant,
        lastUsedCalendar: LastUsedCalendar?
    ) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val label = getString(spectacledVariant.addNewStringRes)

        val shortcuts = buildList {
            add(newEntryShortcut(
                context = context,
                id = SHORTCUT_ID_NEW_ENTRY,
                shortLabel = label,
                longLabel = label,
                icon = addIcon(context, tint = null),
                calendarId = 0L
            ))

            lastUsedCalendar?.let { calendar ->
                // The id carries the calendar id so that a shortcut the user pinned keeps pointing
                // at the calendar it was pinned for: a pinned shortcut follows its id, so a shared
                // one would silently retarget as soon as another calendar becomes the last used one.
                add(newEntryShortcut(
                    context = context,
                    id = SHORTCUT_ID_NEW_ENTRY_IN_CALENDAR_PREFIX + calendar.id,
                    shortLabel = calendar.label,
                    longLabel = getString(spectacledVariant.addNewInCalendarStringRes, calendar.label),
                    icon = addIcon(context, tint = calendar.color),
                    calendarId = calendar.id
                ))
            }
        }

        Napier.d("Publishing ${shortcuts.size} shortcut(s), lastUsedCalendarId: ${lastUsedCalendar?.id}")
        shortcutManager.dynamicShortcuts = shortcuts
    }

    /**
     * The plus icon, tinted with the calendar's color where it has one so that the two shortcuts
     * are told apart by more than their label. The tint is baked into the bitmap instead of left
     * on the [Icon]: ic_add already carries an `android:tint` of its own, and the launcher loads
     * the icon in its own process.
     */
    private fun addIcon(context: Context, tint: Int?): Icon {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_add)
            ?: return Icon.createWithResource(context, R.drawable.ic_add)
        return Icon.createWithBitmap(drawable.mutate().apply { tint?.let { color -> setTint(color) } }.toBitmap())
    }

    private fun newEntryShortcut(
        context: Context,
        id: String,
        shortLabel: String,
        longLabel: String,
        icon: Icon,
        calendarId: Long
    ): ShortcutInfo = ShortcutInfo.Builder(context, id)
        .setShortLabel(shortLabel)
        .setLongLabel(longLabel)
        .setIcon(icon)
        .setIntent(
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                action = Intent.ACTION_VIEW
                putExtra(CALENDAR_ID_KEY, calendarId)
                putExtra(ICAL_ENTRY_ID_KEY, 0L)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            } ?: Intent()
        )
        .build()

    /** What of the last used calendar ends up in its shortcut. [color] is an ARGB int, or null. */
    private data class LastUsedCalendar(val id: Long, val label: String, val color: Int?)

    /**
     * Guaranteed non-blank: `ShortcutInfo.Builder.setShortLabel` rejects an empty label, and a
     * calendar that has not been named yet carries `""` (see `Calendar.getNewCalendar`).
     */
    private fun Calendar.shortcutLabel(): String =
        displayName?.takeIf { it.isNotBlank() }
            ?: url.host.takeIf { it.isNotBlank() }
            ?: url.toString()
}
