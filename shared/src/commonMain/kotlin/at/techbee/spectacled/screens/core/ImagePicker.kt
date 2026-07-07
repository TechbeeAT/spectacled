package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable

interface ImagePicker {
    fun pickImage()
    fun takePhoto()
}

@Composable
expect fun rememberImagePicker(onImagePicked: (PickedFile?) -> Unit): ImagePicker
