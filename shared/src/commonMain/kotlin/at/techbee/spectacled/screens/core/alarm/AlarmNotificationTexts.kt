package at.techbee.spectacled.screens.core.alarm

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.getString
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.alarm
import spectacled.shared.generated.resources.alarm_notification_action_done
import spectacled.shared.generated.resources.alarm_notification_action_snooze
import spectacled.shared.generated.resources.alarm_notification_channel_description
import spectacled.shared.generated.resources.alarm_notification_channel_name

/**
 * The texts of an alarm notification, from the shared string resources so they are translated
 * like the rest of the app. Loading them is suspending, while the platforms build notifications in
 * plain callbacks, hence this bundle. [channelName] and [channelDescription] are only used on
 * Android, which shows them in the app's notification settings.
 */
data class AlarmNotificationTexts(
    /** Title of a notification for an entry without a summary. */
    val defaultTitle: String,
    val markDone: String,
    val snooze: String,
    val channelName: String,
    val channelDescription: String
) {
    companion object {
        /** Only used if the resources can't be read, so a notification never goes out without text. */
        val FALLBACK = AlarmNotificationTexts(
            defaultTitle = "Alarm",
            markDone = "Mark done",
            snooze = "Snooze",
            channelName = "Alarms",
            channelDescription = "Reminders for tasks with an alarm"
        )

        /** Reads the texts in the current locale. */
        suspend fun load(): AlarmNotificationTexts = try {
            AlarmNotificationTexts(
                defaultTitle = getString(Res.string.alarm),
                markDone = getString(Res.string.alarm_notification_action_done),
                snooze = getString(Res.string.alarm_notification_action_snooze),
                channelName = getString(Res.string.alarm_notification_channel_name),
                channelDescription = getString(Res.string.alarm_notification_channel_description)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Napier.w("Alarm notification texts not loaded, using the fallback", e)
            FALLBACK
        }
    }
}
