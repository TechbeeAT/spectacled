package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject

class IosImagePicker(
    private val onImagePicked: (PickedFile?) -> Unit
) : ImagePicker {

    private val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
        @OptIn(ExperimentalForeignApi::class)
        override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            if (image == null) {
                onImagePicked(null)
                picker.dismissViewControllerAnimated(true, null)
                return
            }

            val data = UIImageJPEGRepresentation(image, 0.8)
            if (data == null) {
                onImagePicked(null)
                picker.dismissViewControllerAnimated(true, null)
                return
            }

            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }

            onImagePicked(PickedFile("photo.jpg", bytes, "image/jpeg"))
            picker.dismissViewControllerAnimated(true, null)
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            onImagePicked(null)
            picker.dismissViewControllerAnimated(true, null)
        }
    }

    override fun pickImage() {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
        picker.delegate = delegate

        present(picker)
    }

    override fun takePhoto() {
        if (!UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
            pickImage()
            return
        }

        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.delegate = delegate

        present(picker)
    }

    private fun present(picker: UIViewController) {
        val window = UIApplication.sharedApplication.keyWindow ?: UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
        val rootViewController = window?.rootViewController
        rootViewController?.presentViewController(picker, animated = true, completion = null)
    }
}

@Composable
actual fun rememberImagePicker(onImagePicked: (PickedFile?) -> Unit): ImagePicker {
    return remember { IosImagePicker(onImagePicked) }
}
