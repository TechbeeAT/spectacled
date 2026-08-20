package at.techbee.spectacled.screens.core.domain

import androidx.compose.ui.state.ToggleableState
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IcalEntryTest {

    @Test
    fun noteJournalTaskClassification() {
        val note = IcalEntry.newNote()
        assertTrue(note.isNote())
        assertFalse(note.isJournal())
        assertFalse(note.isTask())

        // A journal is a VJOURNAL *with* a dtStart; a note is one without
        val journal = IcalEntry.newJournal()
        assertTrue(journal.isJournal())
        assertFalse(journal.isNote())
        assertFalse(journal.isTask())

        val task = IcalEntry.newTask()
        assertTrue(task.isTask())
        assertFalse(task.isNote())
        assertFalse(task.isJournal())
    }

    @Test
    fun subtaskRequiresParentUid() {
        assertFalse(IcalEntry.newTask().isSubtask())
        assertTrue(IcalEntry.newTask().copy(parentUid = "parent").isSubtask())
    }

    @Test
    fun isRecurringDetectsRecurrenceProperties() {
        assertFalse(IcalEntry.newJournal().isRecurring())
        // An unrelated extra property must not count as recurrence.
        assertFalse(
            IcalEntry.newJournal().copy(
                extraProperties = listOf(RawIcsProperty(name = "X-FOO", unfoldedLine = "X-FOO:bar"))
            ).isRecurring()
        )

        assertTrue(
            IcalEntry.newJournal().copy(
                extraProperties = listOf(RawIcsProperty(name = "RRULE", unfoldedLine = "RRULE:FREQ=DAILY"))
            ).isRecurring()
        )
        assertTrue(
            IcalEntry.newTask().copy(
                extraProperties = listOf(RawIcsProperty(name = "RDATE", unfoldedLine = "RDATE:20260101T100000Z"))
            ).isRecurring()
        )
        assertTrue(
            IcalEntry.newJournal().copy(
                extraProperties = listOf(RawIcsProperty(name = "RECURRENCE-ID", unfoldedLine = "RECURRENCE-ID:20260101"))
            ).isRecurring()
        )
    }

    @Test
    fun isRecurringMatchesPropertyNamesCaseInsensitively() {
        // RFC 5545 property names are case-insensitive, so a lower-case producer still counts.
        assertTrue(
            IcalEntry.newJournal().copy(
                extraProperties = listOf(RawIcsProperty(name = "rrule", unfoldedLine = "rrule:FREQ=WEEKLY"))
            ).isRecurring()
        )
    }

    @Test
    fun pinnedIsDrivenByThePinCategory() {
        assertFalse(IcalEntry.newNote().isPinned())
        assertFalse(IcalEntry.newNote().copy(categories = listOf("Other")).isPinned())
        assertTrue(IcalEntry.newNote().copy(categories = listOf("Other", IcalEntry.PINNED_CATEGORY)).isPinned())
    }

    @Test
    fun withProgressUpdated_mapsPercentToStatus() {
        val task = IcalEntry.newTask()
        assertNull(task.withProgressUpdated(0L).status)
        assertEquals(Status.IN_PROCESS, task.withProgressUpdated(1L).status)
        assertEquals(Status.IN_PROCESS, task.withProgressUpdated(99L).status)
        assertEquals(Status.COMPLETED, task.withProgressUpdated(100L).status)
        assertEquals(50L, task.withProgressUpdated(50L).percentComplete)
    }

    @Test
    fun withProgressUpdated_marksSyncedEntryAsLocallyModified() {
        val synced = IcalEntry.newTask().copy(syncState = SyncState.SYNCED)
        assertEquals(SyncState.LOCAL_MODIFIED, synced.withProgressUpdated(50L).syncState)
    }

    @Test
    fun withProgressUpdated_doesNotOverwriteConflictState() {
        val conflicted = IcalEntry.newTask().copy(syncState = SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED)
        assertEquals(SyncState.CONFLICT_LOCAL_MODIFIED_SERVER_MODIFIED, conflicted.withProgressUpdated(50L).syncState)
    }

    @Test
    fun progressTriStateFollowsPercentComplete() {
        val task = IcalEntry.newTask()
        assertEquals(ToggleableState.Off, task.copy(percentComplete = 0L).getProgressTriState())
        assertEquals(ToggleableState.Indeterminate, task.copy(percentComplete = 50L).getProgressTriState())
        assertEquals(ToggleableState.On, task.copy(percentComplete = 100L).getProgressTriState())
        assertEquals(ToggleableState.Off, task.copy(percentComplete = 0L, status = null).getProgressTriState())
        assertEquals(ToggleableState.On, task.copy(percentComplete = 0L, status = Status.COMPLETED).getProgressTriState())
    }

    @Test
    fun plainTextForShareWithCategories() {
        val entry = IcalEntry.newNote().copy(
            summary = "S",
            description = "D",
            categories = listOf("A", "B")
        )
        assertEquals("S\nD\n\nCategories: A, B", entry.getPlainTextForShare("Categories"))
    }

    @Test
    fun plainTextForShareWithoutCategories() {
        val entry = IcalEntry.newNote().copy(summary = "S", description = "D")
        assertEquals("S\nD\n", entry.getPlainTextForShare("Categories"))
    }

    @Test
    fun plainTextForShareWithoutSummary() {
        val entry = IcalEntry.newNote().copy(description = "D")
        assertEquals("D\n", entry.getPlainTextForShare("Categories"))
    }
}
