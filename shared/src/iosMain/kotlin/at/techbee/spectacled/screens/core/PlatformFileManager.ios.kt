package at.techbee.spectacled.screens.core

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual class PlatformFileManager : FileManager {
    actual override fun getAttachmentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsDirectory = paths.first() as String
        val attachmentsDir = "$documentsDirectory/attachments"

        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(attachmentsDir)) {
            fileManager.createDirectoryAtPath(attachmentsDir, true, null, null)
        }
        return attachmentsDir
    }

    actual override fun saveAttachment(fileName: String, bytes: ByteArray): String {
        val path = getAttachmentsDirectory() + "/" + fileName
        val data = bytes.usePinned { pinned ->
            NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
        }
        data.writeToFile(path, true)
        return path
    }

    actual override fun deleteAttachment(path: String): Boolean {
        return NSFileManager.defaultManager.removeItemAtPath(path, null)
    }

    actual override fun exists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}
