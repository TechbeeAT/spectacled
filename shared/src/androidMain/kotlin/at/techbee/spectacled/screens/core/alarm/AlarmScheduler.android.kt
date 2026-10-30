package at.techbee.spectacled.screens.core.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import at.techbee.spectacled.shared.R
import at.techbee.spectacled.widget.SpectacledWidget
import io.github.aakira.napier.Napier

private const val NOTIFICATION_CHANNEL_ALARMS = "alarms"

/** Remembers which alarms are pending in AlarmManager, which offers no way to list them. */
private const val PREFS_NAME = "alarm_scheduler"
private const val PREFS_KEY_SCHEDULED = "scheduled_keys"

/**
 * Hands alarms to [AlarmManager], which wakes [AlarmReceiver] at the trigger time whether or not
 * the app is running. Notifications go through one high-importance channel; tapping one opens
 * the entry via the same launch intent the widget uses.
 */
actual class PlatformAlarmScheduler(private val context: Context) : AlarmScheduler {

    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Only reports the state: a runtime permission can't be requested from an application
     * context. Asking is done from the UI via PermissionRequester (AppPermission.NOTIFICATIONS).
     */
    actual override suspend fun requestPermissionIfNeeded(): Boolean = canPostNotifications()

    actual override fun replaceAll(alarms: List<ScheduledAlarm>) {
        val previousKeys = prefs.getStringSet(PREFS_KEY_SCHEDULED, emptySet()) ?: emptySet()
        val newKeys = alarms.map { it.key.asString() }.toSet()

        (previousKeys - newKeys).forEach { stale ->
            alarmManager.cancel(alarmPendingIntent(stale, create = false) ?: return@forEach)
        }

        alarms.forEach { alarm ->
            // Same key, same request code: a re-scheduled alarm replaces its earlier registration.
            val pendingIntent = alarmPendingIntent(alarm.key.asString(), create = true, alarm = alarm)!!
            val triggerMillis = alarm.triggerAt.toEpochMilliseconds()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms())
                AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            else
                // Exact alarms were denied by the user: still fire, within the OS's batching window.
                AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }

        prefs.edit { putStringSet(PREFS_KEY_SCHEDULED, newKeys) }
        Napier.d("AlarmManager: ${alarms.size} alarms scheduled, ${(previousKeys - newKeys).size} cancelled")
    }

    // Lint can't see the permission check inside canPostNotifications().
    @SuppressLint("MissingPermission")
    actual override fun showNow(alarm: ScheduledAlarm) {
        if (!canPostNotifications()) {
            Napier.w("Notification permission missing, alarm for entry ${alarm.icalEntryId} not shown")
            return
        }
        ensureChannel()

        val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra(SpectacledWidget.CALENDAR_ID_KEY, alarm.calendarId)
            putExtra(SpectacledWidget.ICAL_ENTRY_ID_KEY, alarm.icalEntryId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: Intent()

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ALARMS)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(alarm.entrySummary ?: context.getString(R.string.alarm_notification_default_title))
            // The description usually repeats the summary (the app sets it so), then it adds nothing.
            .setContentText(alarm.alarmDescription?.takeIf { it != alarm.entrySummary })
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    alarm.key.notificationId(),
                    contentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        NotificationManagerCompat.from(context).notify(alarm.key.notificationId(), notification)
    }

    actual override fun dismiss(key: ScheduledAlarmKey) {
        NotificationManagerCompat.from(context).cancel(key.notificationId())
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ALARMS,
            context.getString(R.string.alarm_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = context.getString(R.string.alarm_notification_channel_description) }
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    /**
     * The receiver intent for [keyString]. With [create] = false only an existing registration is
     * returned (to cancel it), so a stale key never creates a new PendingIntent just to cancel it.
     */
    private fun alarmPendingIntent(keyString: String, create: Boolean, alarm: ScheduledAlarm? = null): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            // Identifies the registration (filterEquals), not just the extras.
            data = Uri.parse("spectacled-alarm://$keyString")
            alarm?.let {
                putExtra(AlarmReceiver.EXTRA_ENTRY_ID, it.icalEntryId)
                putExtra(AlarmReceiver.EXTRA_ALARM_KEY, it.key.alarmKey)
                putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, it.triggerAt.toEpochMilliseconds())
            }
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
                (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(context, keyString.hashCode(), intent, flags)
    }
}

/** Stable notification id / request code for this alarm, so later calls address the same one. */
internal fun ScheduledAlarmKey.notificationId(): Int = asString().hashCode()
