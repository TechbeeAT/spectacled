package at.techbee.spectacled.screens.core.alarm

import at.techbee.spectacled.DeepLinkHandler
import at.techbee.spectacled.screens.core.PermissionStatus
import at.techbee.spectacled.screens.core.SetTaskDoneUseCase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSystemClockDidChangeNotification
import platform.Foundation.NSSystemTimeZoneDidChangeNotification
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationDefaultActionIdentifier
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.alarm
import spectacled.shared.generated.resources.alarm_notification_action_done
import spectacled.shared.generated.resources.alarm_notification_action_snooze

/** Keys of the notification's userInfo, read back when the user taps it or one of its buttons. */
internal const val USER_INFO_CALENDAR_ID = "calendarId"
internal const val USER_INFO_ENTRY_ID = "entryId"
internal const val USER_INFO_ALARM_KEY = "alarmKey"

/**
 * Notification categories, which decide the buttons an alarm's notification shows: "Mark done"
 * and "Snooze", or only "Snooze" for a task in a calendar that can't be written to.
 */
internal const val CATEGORY_ALARM = "alarm"
internal const val CATEGORY_ALARM_READ_ONLY = "alarm_read_only"
private const val ACTION_MARK_DONE = "markDone"
private const val ACTION_SNOOZE = "snooze"

/**
 * App-wide notification setup on iOS. [install] has to be called from the AppDelegate's
 * didFinishLaunching: UNUserNotificationCenter only reports a tap that launched the app to a
 * delegate that is set before launching finishes.
 */
object IosNotifications : KoinComponent {

    private val alarmCoordinator: AlarmCoordinator by inject()
    private val alarmScheduler: AlarmScheduler by inject()
    private val setTaskDone: SetTaskDoneUseCase by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** For handing results to the UI and navigation, which expect the main thread. */
    internal val mainScope = MainScope()
    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    /** Kept here: the notification center only holds its delegate weakly. */
    private val delegate = NotificationDelegate()

    /**
     * The last authorization status read from the system, null until the first read is back.
     * Reading it is asynchronous on iOS, while PermissionChecker and the scheduler need it right away.
     */
    var authorizationStatus: UNAuthorizationStatus? = null
        private set

    /** Fallback notification title for an entry without a summary, localized once at start. */
    var defaultTitle: String = "Alarm"
        private set

    fun install() {
        center.delegate = delegate
        refreshAuthorizationStatus()
        scope.launch {
            defaultTitle = getString(Res.string.alarm)
            registerCategories()
        }

        val notificationCenter = NSNotificationCenter.defaultCenter
        // Trigger times of all-day and floating dates follow the device zone.
        notificationCenter.addObserverForName(NSSystemTimeZoneDidChangeNotification, null, null) { _ ->
            scope.launch { alarmCoordinator.onTimeOrTimeZoneChanged() }
        }
        // Time-interval triggers count elapsed seconds, so after the clock was set they'd fire at
        // the wrong wall-clock time; rescheduling recomputes the intervals from the new time.
        notificationCenter.addObserverForName(NSSystemClockDidChangeNotification, null, null) { _ ->
            scope.launch { alarmCoordinator.onTimeOrTimeZoneChanged() }
        }
        // The user may have changed the notification permission in the Settings app meanwhile.
        notificationCenter.addObserverForName(UIApplicationDidBecomeActiveNotification, null, null) { _ ->
            refreshAuthorizationStatus()
        }
    }

    fun isAuthorized(): Boolean = authorizationStatus.let {
        it == UNAuthorizationStatusAuthorized || it == UNAuthorizationStatusProvisional || it == UNAuthorizationStatusEphemeral
    }

    /** The user hasn't been asked yet, so [requestAuthorization] will show the system prompt. */
    fun canPrompt(): Boolean = authorizationStatus == UNAuthorizationStatusNotDetermined

    fun permissionStatus(): PermissionStatus = when {
        authorizationStatus == null -> PermissionStatus.UNKNOWN
        isAuthorized() -> PermissionStatus.GRANTED
        else -> PermissionStatus.DENIED     // denied, or not asked yet
    }

    /**
     * Shows the system prompt (only the first time - after a refusal iOS answers without asking)
     * and reports the outcome on the main thread. Once allowed, alarms are scheduled right away by
     * [refreshAuthorizationStatus]: requests added while notifications were not allowed were never
     * registered.
     */
    fun requestAuthorization(onResult: (PermissionStatus) -> Unit) {
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, error ->
            error?.let { Napier.w("Notification authorization failed: ${it.localizedDescription}") }
            refreshAuthorizationStatus { status ->
                mainScope.launch { onResult(status) }
            }
        }
    }

    /**
     * The buttons run in the background (no option to bring the app to the foreground), like on
     * Android. Their titles are localized, hence registered once the strings are loaded.
     */
    private suspend fun registerCategories() {
        val markDone = UNNotificationAction.actionWithIdentifier(
            ACTION_MARK_DONE, getString(Res.string.alarm_notification_action_done), options = 0uL
        )
        val snooze = UNNotificationAction.actionWithIdentifier(
            ACTION_SNOOZE, getString(Res.string.alarm_notification_action_snooze), options = 0uL
        )
        center.setNotificationCategories(
            setOf(
                UNNotificationCategory.categoryWithIdentifier(CATEGORY_ALARM, listOf(markDone, snooze), emptyList<Any>(), options = 0uL),
                UNNotificationCategory.categoryWithIdentifier(CATEGORY_ALARM_READ_ONLY, listOf(snooze), emptyList<Any>(), options = 0uL)
            )
        )
    }

    /**
     * The user tapped the notification ([UNNotificationDefaultActionIdentifier]) or one of its
     * buttons. [onDone] has to be called once handled: iOS keeps the app running until then when it
     * was woken in the background for a button.
     */
    internal fun onNotificationResponse(actionIdentifier: String, calendarId: Long?, entryId: Long, alarmKey: String?, onDone: () -> Unit) {
        when {
            actionIdentifier == ACTION_MARK_DONE && alarmKey != null -> scope.launch {
                try {
                    // Done removes the entry's notifications. If it can't be done (deleted
                    // meanwhile, read-only), at least take this one away - the button did nothing.
                    if (!setTaskDone(entryId, done = true))
                        alarmScheduler.dismiss(ScheduledAlarmKey(entryId, alarmKey))
                } finally {
                    onDone()
                }
            }
            actionIdentifier == ACTION_SNOOZE && alarmKey != null -> scope.launch {
                try {
                    alarmCoordinator.snooze(entryId, alarmKey)
                } finally {
                    onDone()
                }
            }
            actionIdentifier == UNNotificationDefaultActionIdentifier -> {
                // Opens the entry, through the same path as a deep link.
                mainScope.launch { DeepLinkHandler.onDeepLinkReceived(calendarId, entryId, null) }
                onDone()
            }
            else -> onDone()
        }
    }

    private fun refreshAuthorizationStatus(onDone: (PermissionStatus) -> Unit = {}) {
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val wasAuthorized = isAuthorized()
            authorizationStatus = settings?.authorizationStatus
            // Allowed in the Settings app: schedule what couldn't be scheduled before.
            if (!wasAuthorized && isAuthorized())
                scope.launch { alarmCoordinator.reconcile() }
            onDone(permissionStatus())
        }
    }
}

private class NotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {

    /** Without this, iOS doesn't show a notification while the app is in the foreground. */
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound
        )
    }

    /** A tap on the notification opens its entry; its buttons mark the task done or snooze it. */
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        val calendarId = (userInfo[USER_INFO_CALENDAR_ID] as? String)?.toLongOrNull()
        val entryId = (userInfo[USER_INFO_ENTRY_ID] as? String)?.toLongOrNull()
        val alarmKey = userInfo[USER_INFO_ALARM_KEY] as? String
        if (entryId == null) {
            withCompletionHandler()
            return
        }
        IosNotifications.onNotificationResponse(
            didReceiveNotificationResponse.actionIdentifier, calendarId, entryId, alarmKey, withCompletionHandler
        )
    }
}
