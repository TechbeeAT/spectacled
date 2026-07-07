package at.techbee.spectacled.screens.core

interface FileLauncher {
    fun openFile(path: String, mimeType: String?)
    fun openFile(bytes: ByteArray, fileName: String, mimeType: String?)
}

expect class PlatformFileLauncher : FileLauncher {
    override fun openFile(path: String, mimeType: String?)
    override fun openFile(bytes: ByteArray, fileName: String, mimeType: String?)
}
