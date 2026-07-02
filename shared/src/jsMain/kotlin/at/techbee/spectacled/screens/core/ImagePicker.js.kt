package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.files.FileReader
import org.w3c.files.get
import org.w3c.dom.HTMLInputElement

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedFile?) -> Unit): ImagePicker {
    return remember {
        object : ImagePicker {
            override fun pickImage() {
                val input = document.createElement("input") as HTMLInputElement
                input.type = "file"
                input.accept = "image/*"
                input.onchange = {
                    val file = input.files?.get(0)
                    if (file != null) {
                        val reader = FileReader()
                        reader.onload = {
                            val arrayBuffer = reader.result as ArrayBuffer
                            val uint8Array = Uint8Array(arrayBuffer)
                            val bytes = ByteArray(uint8Array.length) { i -> uint8Array.get(i) }
                            onImagePicked(PickedFile(file.name, bytes, file.type))
                        }
                        reader.readAsArrayBuffer(file)
                    } else {
                        onImagePicked(null)
                    }
                }
                input.click()
            }

            override fun takePhoto() {
                pickImage()
            }
        }
    }
}
