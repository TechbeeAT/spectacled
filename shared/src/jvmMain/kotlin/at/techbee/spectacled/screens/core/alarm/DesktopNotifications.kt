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
 * - macOS: `osascript display notification` (Notification Center). AWT's tray messages would add
 *   an icon to the menu bar just for this.
 * - Windows (and Linux desktops with a tray): a message of an AWT tray icon. Clicking it calls
 *   [show]'s onClick.
 * - Linux without a tray: `notify-send`, if installed.
 * None of them can retract a notification or offer buttons.
 */
internal object DesktopNotifications {

    private val os = System.getProperty("os.name").orEmpty().lowercase()
    private var trayIcon: TrayIcon? = null
    @Volatile private var onTrayMessageClick: () -> Unit = {}

    /** Returns false if no way to notify was found or it failed. */
    fun show(appName: String, title: String, text: String?, onClick: () -> Unit): Boolean = try {
        when {
            os.contains("mac") -> run(
                // Arguments instead of string interpolation: nothing in the texts needs escaping.
                "osascript",
                "-e", "on run argv",
                "-e", "display notification (item 2 of argv) with title (item 1 of argv)",
                "-e", "end run",
                title, text.orEmpty()
            )
            SystemTray.isSupported() -> {
                onTrayMessageClick = onClick
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
        icon.addActionListener { onTrayMessageClick() }
        SystemTray.getSystemTray().add(icon)
        trayIcon = icon
    }

    /** The window's icon, as set by the app's main(). */
    private fun appIcon(): Image =
        Window.getWindows().firstNotNullOfOrNull { it.iconImages.firstOrNull() }
            ?: BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)

    private fun run(vararg command: String): Boolean {
        val executable = command.first()
        val process = ProcessBuilder(*command).redirectErrorStream(true).start()
        // Not waited for: showNow runs under AlarmCoordinator's lock, and a slow osascript would
        // hold up every save and sync behind it. Started counts as shown; a failure is only logged.
        process.onExit().thenAccept { exited ->
            if (exited.exitValue() != 0)
                Napier.w("$executable exited with ${exited.exitValue()}: ${exited.inputStream.bufferedReader().readText().take(500)}")
        }
        return true
    }
}
