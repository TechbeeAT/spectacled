package at.techbee.spectacled.screens.core

import androidx.compose.runtime.Composable

data class PickedFile(
    val name: String,
    val bytes: ByteArray,
    val mimeType: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PickedFile
        if (name != other.name) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (mimeType != other.mimeType) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}

interface FilePicker {
    fun pickFile()
}

@Composable
expect fun rememberFilePicker(onFilePicked: (PickedFile?) -> Unit): FilePicker
