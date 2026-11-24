package at.techbee.spectacled.screens.core.alarm

import at.techbee.spectacled.DeepLinkHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Best effort in the browser: alarms fire only while the app's tab is open ([InAppAlarmTimer]);
 * those missed while it was closed are shown when it is opened again. Notifications go through the
 * browser's Notification API ([WebNotifications]); a click focuses the tab and opens the entry.
 */
actual class PlatformAlarmScheduler : AlarmScheduler, KoinComponent {

    private val alarmCoordinator: AlarmCoordinator by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Loading the texts suspends, showing a notification doesn't - so they're loaded up front. */
    private var texts = AlarmNotificationTexts.FALLBACK

    /** Shown notifications by request key, so [dismiss] can close them; dropped once they close. */
    private val shown = mutableMapOf<String, JsAny>()

    private val timer = InAppAlarmTimer(scope) { alarm ->
        alarmCoordinator.onAlarmFired(alarm.icalEntryId, alarm.key.alarmKey, alarm.triggerAt)
    }

    init {
        scope.launch { texts = AlarmNotificationTexts.load() }
    }

    actual override fun replaceAll(alarms: List<ScheduledAlarm>) = timer.replaceAll(alarms)

    actual override fun showNow(alarm: ScheduledAlarm): Boolean {
        val key = alarm.key.asString()
        var notification: JsAny? = null
        notification = WebNotifications.show(
            title = alarm.entrySummary?.takeIf { it.isNotBlank() } ?: texts.defaultTitle,
            text = alarm.entryDescription?.takeIf { it.isNotBlank() },
            tag = key,
            onClick = { DeepLinkHandler.onDeepLinkReceived(alarm.calendarId, alarm.icalEntryId, null) },
            // Only this one: a later notification for the same alarm replaces it and closes it.
            onClose = { if (shown[key] === notification) shown.remove(key) }
        ) ?: return false
        shown[key] = notification
        return true
    }

    actual override fun dismiss(key: ScheduledAlarmKey) {
        shown.remove(key.asString())?.let { WebNotifications.close(it) }
    }
}
