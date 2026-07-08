package at.techbee.spectacled.screens.core.mapper.dto

import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.sqldelight.AttachmentDto

fun AttachmentDto.toDomain(): Attachment {
    return Attachment(
        id = this.id,
        icalEntryId = this.icalEntryId,
        uid = this.uid,
        localPath = this.localPath,
        remoteUrl = this.remoteUrl,
        fileName = this.fileName,
        mimeType = this.mimeType,
        size = this.size,
        isInline = this.isInline != 0L,
        syncState = AttachmentSyncState.valueOf(this.syncState)
    )
}

fun Attachment.toDto(): AttachmentDto {
    return AttachmentDto(
        id = this.id,
        icalEntryId = this.icalEntryId,
        uid = this.uid,
        localPath = this.localPath,
        remoteUrl = this.remoteUrl,
        fileName = this.fileName,
        mimeType = this.mimeType,
        size = this.size,
        isInline = if (this.isInline) 1L else 0L,
        syncState = this.syncState.name
    )
}
