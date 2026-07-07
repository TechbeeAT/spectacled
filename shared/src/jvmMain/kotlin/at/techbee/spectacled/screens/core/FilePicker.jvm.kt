package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

@Composable
actual fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): FilePicker {
    return remember {
        object : FilePicker {
            override fun pickFile() {
                SwingUtilities.invokeLater {
                    val fileChooser = JFileChooser()
                    val result = fileChooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val file = fileChooser.selectedFile
                        onFilePicked(PickedFile(file.name, file.readBytes(), null))
                    } else {
                        onFilePicked(null)
                    }
                }
            }
        }
    }
}
