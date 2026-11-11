@file:OptIn(ExperimentalWasmJsInterop::class)

package at.techbee.spectacled.screens.core.alarm

import at.techbee.spectacled.screens.core.PermissionStatus

/**
 * The browser's Notification API. Notifications only appear while the app's tab is open (no
 * service worker or push), and the browser decides how they look - no buttons here.
 */
internal object WebNotifications {

    /**
     * GRANTED or DENIED as the browser reports it; not asked yet counts as DENIED, so the app offers
     * to ask. NOT_APPLICABLE where the browser has no Notification API (or not in this context).
     */
    fun permissionStatus(): PermissionStatus = when (notificationPermission()) {
        "granted" -> PermissionStatus.GRANTED
        "denied", "default" -> PermissionStatus.DENIED
        else -> PermissionStatus.NOT_APPLICABLE
    }

    /** The browser only prompts while it wasn't decided yet; after a refusal it answers right away. */
    fun canPrompt(): Boolean = notificationPermission() == "default"

    fun requestPermission(onResult: (PermissionStatus) -> Unit) =
        requestNotificationPermission { onResult(permissionStatus()) }

    /** Returns the shown notification, for [close], or null if it couldn't be shown (not allowed, not supported). */
    fun show(title: String, text: String?, tag: String, onClick: () -> Unit): JsAny? {
        if (permissionStatus() != PermissionStatus.GRANTED) return null
        // The tag makes a notification for the same alarm replace an earlier one.
        return showNotification(title, text.orEmpty(), tag, onClick)
    }

    fun close(notification: JsAny) = closeNotification(notification)
}

@JsFun("() => (typeof Notification === 'undefined') ? 'unsupported' : Notification.permission")
private external fun notificationPermission(): String

@JsFun("(onDone) => { Notification.requestPermission().then(() => onDone(), () => onDone()); }")
private external fun requestNotificationPermission(onDone: () -> Unit)

// The constructor throws where pages may only notify through a service worker (e.g. Chrome on
// Android) even with the permission granted - reported as not shown instead.
@JsFun("""(title, body, tag, onClick) => {
    try {
        const notification = new Notification(title, { body: body, tag: tag });
        notification.onclick = () => { window.focus(); notification.close(); onClick(); };
        return notification;
    } catch (e) {
        console.warn("Alarm notification not shown", e);
        return null;
    }
}""")
private external fun showNotification(title: String, body: String, tag: String, onClick: () -> Unit): JsAny?

@JsFun("(notification) => notification.close()")
private external fun closeNotification(notification: JsAny)
