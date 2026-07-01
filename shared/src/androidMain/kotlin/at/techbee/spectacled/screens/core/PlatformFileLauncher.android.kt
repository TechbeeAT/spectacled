package at.techbee.spectacled.screens.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

actual class PlatformFileLauncher(private val context: Context) : FileLauncher {
    actual override fun openFile(path: String, mimeType: String?) {
        try {
            val file = File(path)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual override fun openFile(bytes: ByteArray, fileName: String, mimeType: String?) {
        val path = PlatformFileManager(context).saveAttachment(fileName, bytes)
        openFile(path, mimeType)
    }
}
