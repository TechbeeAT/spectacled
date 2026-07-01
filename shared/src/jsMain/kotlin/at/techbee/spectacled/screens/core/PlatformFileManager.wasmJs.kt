package at.techbee.spectacled.screens.core

actual class PlatformFileManager : FileManager {
    actual override fun getAttachmentsDirectory(): String = ""

    actual override fun saveAttachment(fileName: String, bytes: ByteArray): String = ""

    actual override fun deleteAttachment(path: String): Boolean = false

    actual override fun exists(path: String): Boolean = false
}
