package at.techbee.spectacled.screens.core.alarm

import io.github.aakira.napier.Napier
import java.awt.EventQueue
import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Window
import java.awt.image.BufferedImage

/**
 * Shows a notification through what the desktop OS offers a JVM app:
 * - macOS, Windows and Linux desktops with a tray: a message of an AWT tray icon. On macOS it
 *   appears in Notification Center under the app's name (`osascript` would post it as Script
 *   Editor's, and a click would open Script Editor); the price is an icon in the menu bar while the
 *   app runs. Clicking a message calls [show]'s onClick, as far as it can tell which message was
 *   clicked (see [onTrayMessageClick]).
 * - Linux without a tray: `notify-send`, if installed.
 * None of them can retract a notification or offer buttons.
 */
internal object DesktopNotifications {

    private val os = System.getProperty("os.name").orEmpty().lowercase()
    private var trayIcon: TrayIcon? = null

    /**
     * A tray icon reports a click on any of its messages without saying which one, so the entry is
     * only opened while a single message was shown since the last click. With more, the click only
     * brings the app to the front rather than risk opening the wrong entry.
     */
    private var onTrayMessageClick: () -> Unit = {}
    private var trayMessagesSinceClick = 0

    /** Returns false if no way to notify was found or it failed. */
    fun show(appName: String, title: String, text: String?, onClick: () -> Unit): Boolean = try {
        when {
            SystemTray.isSupported() -> {
                synchronized(this) {
                    onTrayMessageClick = onClick
                    trayMessagesSinceClick++
                }
                trayIcon(appName).displayMessage(title, text.orEmpty(), TrayIcon.MessageType.NONE)
                true
            }
            os.contains("linux") -> run("notify-send", "--app-name=$appName", title, text.orEmpty())
            else -> false
        }
    } catch (e: Exception) {
        Napier.w("Desktop notification not shown", e)
        false
    }

    /** Brings the app's window to the front, e.g. after a click on a notification. */
    fun focusApp() = EventQueue.invokeLater {
        Window.getWindows().filter { it.isVisible }.forEach { it.toFront() }
    }

    @Synchronized
    private fun trayIcon(appName: String): TrayIcon = trayIcon ?: TrayIcon(appIcon(), appName).also { icon ->
        icon.isImageAutoSize = true
        icon.addActionListener { onTrayClick() }
        SystemTray.getSystemTray().add(icon)
        trayIcon = icon
    }

    private fun onTrayClick() {
        val (onClick, shownSinceClick) = synchronized(this) {
            (onTrayMessageClick to trayMessagesSinceClick).also { trayMessagesSinceClick = 0 }
        }
        if (shownSinceClick == 1) onClick() else focusApp()
    }

    /** The window's icon, as set by the app's main(). */
    private fun appIcon(): Image =
        Window.getWindows().firstNotNullOfOrNull { it.iconImages.firstOrNull() }
            ?: BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)

    private fun run(vararg command: String): Boolean {
        val executable = command.first()
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        // Not waited for: showNow runs under AlarmCoordinator's lock, and a slow notify-send would
        // hold up every save and sync behind it. Started counts as shown; a failure is only logged.
        process.onExit().thenAccept { exited ->
            if (exited.exitValue() != 0)
                Napier.w("$executable exited with ${exited.exitValue()}: ${exited.inputStream.bufferedReader().readText().take(500)}")
        }
        return true
    }
}
