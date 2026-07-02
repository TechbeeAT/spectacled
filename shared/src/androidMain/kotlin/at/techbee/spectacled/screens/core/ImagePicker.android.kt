package at.techbee.spectacled.screens.core

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedFile?) -> Unit): ImagePicker {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImagePicked(uriToPickedFile(context, uri))
        } else {
            onImagePicked(null)
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            onImagePicked(uriToPickedFile(context, photoUri!!))
        } else {
            onImagePicked(null)
        }
    }

    return remember {
        object : ImagePicker {
            override fun pickImage() {
                pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            override fun takePhoto() {
                val photoFile = File.createTempFile("TEMP_PHOTO_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                photoUri = uri
                takePhotoLauncher.launch(uri)
            }
        }
    }
}

private fun uriToPickedFile(context: Context, uri: Uri): PickedFile? {
    return try {
        val contentResolver = context.contentResolver
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
        val name = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "photo.jpg"
        val mimeType = contentResolver.getType(uri)

        if (bytes != null) {
            PickedFile(name, bytes, mimeType)
        } else null
    } catch (_: Exception) {
        null
    }
}
