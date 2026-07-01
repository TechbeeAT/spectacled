package at.techbee.spectacled.screens.core

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): FilePicker {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            onFilePicked(null)
            return@rememberLauncherForActivityResult
        }

        try {
            val contentResolver = context.contentResolver
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            } ?: "unknown"
            val mimeType = contentResolver.getType(uri)

            if (bytes != null) {
                onFilePicked(PickedFile(name, bytes, mimeType))
            } else {
                onFilePicked(null)
            }
        } catch (_: Exception) {
            onFilePicked(null)
        }
    }

    return remember {
        object : FilePicker {
            override fun pickFile() {
                launcher.launch("*/*")
            }
        }
    }
}
