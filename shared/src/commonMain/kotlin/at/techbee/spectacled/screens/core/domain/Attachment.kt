package at.techbee.spectacled.screens.core.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class AttachmentSyncState {
    LOCAL_MODIFIED,
    SYNCED,
    PENDING_DOWNLOAD
}

const val MIMETYPE_SVG = "image/svg+xml"

@OptIn(ExperimentalUuidApi::class)
data class Attachment(
    val id: Long = 0L,
    val icalEntryId: Long = 0L,
    val uid: String = Uuid.random().toString(),
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val size: Long? = null,
    val isInline: Boolean = false,
    val syncState: AttachmentSyncState = AttachmentSyncState.LOCAL_MODIFIED
) {
    fun isSVG() = mimeType == MIMETYPE_SVG

    fun isImage() = mimeType?.startsWith("image/") == true
}