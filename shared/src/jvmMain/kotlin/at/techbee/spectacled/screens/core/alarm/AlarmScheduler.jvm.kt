package at.techbee.spectacled.screens.core.alarm

import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.SpectacledVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Best effort on desktop: alarms fire only while the app runs ([InAppAlarmTimer]); those missed
 * while it was closed are shown at the next start. Notifications go through
 * [DesktopNotifications], without "Mark done"/"Snooze" buttons, which the OS mechanisms
 * available to a JVM app don't offer. A click (where supported) opens the entry.
 */
actual class PlatformAlarmScheduler : AlarmScheduler, KoinComponent {

    private val alarmCoordinator: AlarmCoordinator by inject()
    private val spectacledVariant: SpectacledVariant by inject()

    private val timer = InAppAlarmTimer(CoroutineScope(SupervisorJob() + Dispatchers.Default)) { alarm ->
        alarmCoordinator.onAlarmFired(alarm.icalEntryId, alarm.key.alarmKey, alarm.triggerAt)
    }

    actual override fun replaceAll(alarms: List<ScheduledAlarm>) = timer.replaceAll(alarms)

    actual override fun showNow(alarm: ScheduledAlarm): Boolean {
        // Runs off the main thread (AlarmCoordinator's dispatcher), so blocking for the texts is fine.
        val (texts, appName) = runBlocking { AlarmNotificationTexts.load() to getString(spectacledVariant.appNameStringRes) }
        return DesktopNotifications.show(
            appName = appName,
            title = alarm.entrySummary?.takeIf { it.isNotBlank() } ?: texts.defaultTitle,
            text = alarm.entryDescription?.takeIf { it.isNotBlank() }
        ) {
            DesktopNotifications.focusApp()
            DeepLinkHandler.onDeepLinkReceived(alarm.calendarId, alarm.icalEntryId, null)
        }
    }

    /** Notifications the desktop OS shows can't be taken back by the app; they expire on their own. */
    actual override fun dismiss(key: ScheduledAlarmKey) = Unit
}
