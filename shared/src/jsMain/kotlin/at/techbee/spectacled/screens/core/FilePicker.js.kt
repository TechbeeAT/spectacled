package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
actual fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): FilePicker {
    return remember {
        object : FilePicker {
            override fun pickFile() {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.onchange = {
                    val file = input.files?.get(0)
                    if (file != null) {
                        val reader = FileReader()
                        reader.onload = {
                            val arrayBuffer = reader.result as ArrayBuffer
                            val uint8Array = Uint8Array(arrayBuffer)
                            val bytes = ByteArray(uint8Array.length) { i -> uint8Array.get(i) }
                            onFilePicked(PickedFile(file.name, bytes, file.type))
                        }
                        reader.readAsArrayBuffer(file)
                    } else {
                        onFilePicked(null)
                    }
                }
                input.click()
            }
        }
    }
}
