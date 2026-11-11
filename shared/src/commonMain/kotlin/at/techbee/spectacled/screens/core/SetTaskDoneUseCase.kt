package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.alarm.AlarmCoordinator
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository

/**
 * Checks or unchecks a task from outside its screens - the widget's checkbox and the "Mark done"
 * action of an alarm notification. Same update as toggling the checkbox in the list, so e.g. a
 * conflict state is kept instead of being overwritten with LOCAL_MODIFIED.
 */
class SetTaskDoneUseCase(
    private val icalEntryRepository: IcalEntryRepository,
    private val alarmCoordinator: AlarmCoordinator,
    private val syncTrigger: SyncTrigger,
) {

    /** Returns false if nothing was changed: the entry is gone, not a task, or read-only. */
    suspend operator fun invoke(icalEntryId: Long, done: Boolean): Boolean {
        val icalEntry = icalEntryRepository.getIcalEntryById(icalEntryId) ?: return false
        // Deleted and recurring entries are read-only (this app has no recurrence support), so
        // ignore the request even if a stale widget or notification still offers it.
        if (!icalEntry.isTask() || icalEntry.syncState.isDeletedState() || icalEntry.isRecurring())
            return false

        val updatedEntry = icalEntry.withProgressUpdated(if (done) 100L else 0L)
        icalEntryRepository.updateProgress(
            id = updatedEntry.id,
            percentComplete = updatedEntry.percentComplete,
            status = updatedEntry.status,
            lastModified = updatedEntry.lastModified,
            syncState = updatedEntry.syncState
        )
        if (updatedEntry.isDoneOrCancelled())
            alarmCoordinator.onEntriesDoneOrDeleted(listOf(updatedEntry.id))
        else
            alarmCoordinator.reconcile()
        syncTrigger.requestImmediate(listOf(updatedEntry.calendarId))
        return true
    }
}
