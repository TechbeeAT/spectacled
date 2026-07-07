package at.techbee.spectacled.screens.core

import java.awt.Desktop
import java.io.File

actual class PlatformFileLauncher : FileLauncher {
    actual override fun openFile(path: String, mimeType: String?) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(File(path))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual override fun openFile(bytes: ByteArray, fileName: String, mimeType: String?) {
        val path = PlatformFileManager().saveAttachment(fileName, bytes)
        openFile(path, mimeType)
    }
}
