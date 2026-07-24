package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.CredentialStore
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.webdav.WebDavRemoteIcalEntryDataSource
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import io.github.aakira.napier.Napier
import io.ktor.http.Url

/**
 * Moves entries (and, recursively, their subtasks) to another calendar. Shared by the details and
 * list screens.
 *
 * CalDAV has no reliable cross-collection move, so the repository re-creates each entry in the
 * target collection and marks the original deleted. Before that, this use case makes sure every
 * attachment has a local copy so attachments that only live on the server are physically
 * re-uploaded into the target collection instead of being left behind as a dangling reference.
 * Finally it triggers a sync of every affected collection.
 */
class MoveIcalEntriesUseCase(
    private val icalEntryRepository: IcalEntryRepository,
    private val calendarRepository: CalendarRepository,
    private val credentialStore: CredentialStore,
    private val remoteIcalEntryDataSource: WebDavRemoteIcalEntryDataSource,
    private val fileManager: FileManager,
    private val syncTrigger: SyncTrigger,
) {

    suspend fun move(icalEntryIds: List<Long>, targetCalendarId: Long) {

        if(icalEntryIds.isEmpty())
            return

        val entries = icalEntryRepository.getIcalEntriesWithSubtasks(icalEntryIds)

        ensureAttachmentsDownloaded(entries)

        icalEntryRepository.moveIcalEntriesToCalendar(icalEntryIds, targetCalendarId)

        val affectedCalendarIds = (entries.map { it.calendarId } + targetCalendarId).distinct()
        syncTrigger.requestImmediate(affectedCalendarIds)
        syncTrigger.triggerWidgetUpdate()
    }

    // Best-effort: downloads any server-only attachment (no local copy) of the given entries and
    // persists its local path. A failure leaves the attachment as remote-only, in which case the
    // move keeps its existing server reference instead of losing it.
    private suspend fun ensureAttachmentsDownloaded(entries: List<IcalEntry>) {

        val credentialsByCalendar = mutableMapOf<Long, Credentials?>()

        entries.forEach { entry ->
            entry.attachments.forEach { attachment ->
                val remoteUrl = attachment.remoteUrl
                if(attachment.localPath != null || remoteUrl == null || remoteUrl.isBlank())
                    return@forEach

                try {
                    val credentials = credentialsByCalendar.getOrPut(entry.calendarId) {
                        calendarRepository.getPrincipalUrlForCalendarId(entry.calendarId)
                            ?.let { credentialStore.load(Url(it)) }
                    } ?: return@forEach

                    val bytes = remoteIcalEntryDataSource.downloadFile(Url(remoteUrl), credentials) ?: return@forEach
                    val localPath = fileManager.saveAttachment("${attachment.uid}_${attachment.fileName ?: "file"}", bytes)
                    icalEntryRepository.insertOrUpdateAttachment(
                        attachment.copy(localPath = localPath, syncState = AttachmentSyncState.SYNCED)
                    )
                } catch (e: Exception) {
                    Napier.d("Move pre-download failed for attachment ${attachment.uid}: ${e.message}")
                }
            }
        }
    }
}
