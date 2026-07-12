package at.techbee.spectacled.screens.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncStateAndStatusTest {

    @Test
    fun deletedStates_exhaustive() {
        val expectedDeleted = setOf(SyncState.LOCAL_DELETED, SyncState.REMOTE_DELETED_LOCAL_TRASHBIN)
        SyncState.entries.forEach { state ->
            assertEquals(state in expectedDeleted, state.isDeletedState(), "isDeletedState() for $state")
        }
    }

    @Test
    fun conflictStates_exhaustive() {
        val expectedConflicts = setOf(
            SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED,
            SyncState.CONFLICT_LOCAL_DELETED_SERVER_MODIFIED,
            SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_DELETED
        )
        SyncState.entries.forEach { state ->
            assertEquals(state in expectedConflicts, state.isConflictState(), "isConflictState() for $state")
        }
    }

    @Test
    fun statusEntriesPerComponent() {
        assertEquals(
            listOf(Status.DRAFT, Status.FINAL, Status.CANCELLED),
            Status.entriesForComponent(CalendarComponent.VJOURNAL)
        )
        assertEquals(
            listOf(Status.NEEDS_ACTION, Status.IN_PROCESS, Status.COMPLETED, Status.CANCELLED),
            Status.entriesForComponent(CalendarComponent.VTODO)
        )
        assertEquals(emptyList<Status>(), Status.entriesForComponent(CalendarComponent.VEVENT))
    }
}
