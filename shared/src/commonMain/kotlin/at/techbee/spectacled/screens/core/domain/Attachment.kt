package at.techbee.spectacled.screens.core.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class AttachmentSyncState {
    LOCAL_MODIFIED,
    SYNCED,
    PENDING_DOWNLOAD,
    FAILED
}

const val MIMETYPE_SVG = "image/svg+xml"

/**
 * Attachments are embedded inline (BASE64) inside the iCalendar object, so they sync to any
 * CalDAV server without needing server-side attachment support. The trade-off is that the whole
 * object — inflated ~33% by BASE64 — is re-uploaded on every edit, and CalDAV servers cap object
 * size. These thresholds guard against that:
 *  - above [INLINE_ATTACHMENT_WARN_BYTES] the file is still added, but the user is warned;
 *  - above [MAX_INLINE_ATTACHMENT_BYTES] the file is rejected.
 * Kept as raw-byte constants (not yet a user setting) so they're easy to surface in settings later.
 */
const val MAX_INLINE_ATTACHMENT_BYTES = 5L * 1024 * 1024   // 5 MiB raw (~6.7 MiB once BASE64-encoded)
const val INLINE_ATTACHMENT_WARN_BYTES = 1L * 1024 * 1024   // 1 MiB raw

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
    val syncState: AttachmentSyncState = AttachmentSyncState.LOCAL_MODIFIED,
    val syncErrorMessage: String? = null
) {
    fun isSVG() = mimeType == MIMETYPE_SVG

    fun isImage() = mimeType?.startsWith("image/") == true
}