package at.techbee.spectacled.screens.core.domain

/**
 * A remote attachment blob that is no longer referenced by any entry and still needs to be deleted
 * from its CalDAV collection. Queued locally and drained by the sync engine.
 */
data class PendingRemoteFileDeletion(
    val id: Long,
    val calendarId: Long,
    val remoteUrl: String
)
