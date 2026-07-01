package at.techbee.spectacled.screens.core

interface FileManager {
    fun getAttachmentsDirectory(): String
    fun saveAttachment(fileName: String, bytes: ByteArray): String
    fun readAttachment(path: String): ByteArray
    fun deleteAttachment(path: String): Boolean
    fun exists(path: String): Boolean
}

expect class PlatformFileManager : FileManager {
    override fun getAttachmentsDirectory(): String
    override fun saveAttachment(fileName: String, bytes: ByteArray): String
    override fun readAttachment(path: String): ByteArray
    override fun deleteAttachment(path: String): Boolean
    override fun exists(path: String): Boolean
}
