package at.techbee.spectacled.screens.core

import java.io.File

actual class PlatformFileManager : FileManager {
    actual override fun getAttachmentsDirectory(): String {
        val dir = File(System.getProperty("user.home"), ".spectacled/attachments")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    actual override fun saveAttachment(fileName: String, bytes: ByteArray): String {
        val file = File(getAttachmentsDirectory(), fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual override fun readAttachment(path: String): ByteArray {
        return File(path).readBytes()
    }

    actual override fun deleteAttachment(path: String): Boolean {
        return File(path).delete()
    }

    actual override fun exists(path: String): Boolean {
        return File(path).exists()
    }
}
