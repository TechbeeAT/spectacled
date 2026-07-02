package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedFile?) -> Unit): ImagePicker {
    return remember {
        object : ImagePicker {
            override fun pickImage() {
                SwingUtilities.invokeLater {
                    val fileChooser = JFileChooser()
                    fileChooser.fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif")
                    val result = fileChooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val file = fileChooser.selectedFile
                        onImagePicked(PickedFile(file.name, file.readBytes(), null))
                    } else {
                        onImagePicked(null)
                    }
                }
            }

            override fun takePhoto() {
                pickImage()
            }
        }
    }
}
