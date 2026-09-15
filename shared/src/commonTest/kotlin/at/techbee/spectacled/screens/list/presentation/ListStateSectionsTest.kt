package at.techbee.spectacled.screens.list.presentation

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSection
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListStateSectionsTest {

    /** The notes list is built as pinned -> grouped -> no-criteria -> trashbin, and a dated journal
     *  (wrong type for the notes app) is pulled into the no-criteria section. */
    @Test
    fun notesSectionsAreOrderedAndSplitByType() {
        val pinnedNote = IcalEntry.newNote().copy(categories = listOf(IcalEntry.PINNED_CATEGORY))
        val plainNote = IcalEntry.newNote()
        val datedJournal = IcalEntry.newJournal()            // wrong type for the notes app
        val deletedNote = IcalEntry.newNote().copy(syncState = SyncState.LOCAL_DELETED)

        val state = ListState(
            icalEntries = listOf(pinnedNote, plainNote, datedJournal, deletedNote),
            spectacledVariant = SpectacledVariant.NOTES
        ).recompute()

        assertEquals(
            listOf(
                ListSection.Kind.PINNED,
                ListSection.Kind.GROUPED,
                ListSection.Kind.NO_CRITERIA,
                ListSection.Kind.TRASHBIN
            ),
            state.sections.map { it.kind }
        )

        assertEquals(listOf(pinnedNote.uid), state.sections.first { it.kind == ListSection.Kind.PINNED }.entries.map { it.uid })
        assertEquals(listOf(datedJournal.uid), state.sections.first { it.kind == ListSection.Kind.NO_CRITERIA }.entries.map { it.uid })
        assertEquals(listOf(deletedNote.uid), state.sections.first { it.kind == ListSection.Kind.TRASHBIN }.entries.map { it.uid })

        // The body feed (drag/select-all) excludes pinned and trashbin, but keeps the no-criteria entry.
        assertEquals(setOf(plainNote.uid, datedJournal.uid), state.displayedEntries.map { it.uid }.toSet())
        assertFalse(state.isDisplayEmpty)
    }

    /** Sorting tasks by due date routes a task without a due date to the no-criteria section instead
     *  of bucketing it under "now" (the previous `?: IcsDateTime.now()` behaviour). */
    @Test
    fun taskWithoutDueDateGoesToNoCriteria() {
        val taskWithDue = IcalEntry.newTask().copy(due = IcsDateTime.now())
        val taskNoDue = IcalEntry.newTask().copy(due = null)

        val state = ListState(
            icalEntries = listOf(taskWithDue, taskNoDue),
            spectacledVariant = SpectacledVariant.TASKS,
            listSortedBy = ListSortedBy.DUE
        ).recompute()

        val noCriteria = state.sections.first { it.kind == ListSection.Kind.NO_CRITERIA }
        assertEquals(listOf(taskNoDue.uid), noCriteria.entries.map { it.uid })

        val grouped = state.sections.filter { it.kind == ListSection.Kind.GROUPED }
        assertTrue(grouped.isNotEmpty())
        assertTrue(grouped.none { section -> section.entries.any { it.uid == taskNoDue.uid } })
        assertTrue(grouped.any { section -> section.entries.any { it.uid == taskWithDue.uid } })
    }

    /** Sorted by drag-and-drop, the entries the list renders are the filtered ones: this is the feed the
     *  view model copies into its drag-and-drop list, so a filter that didn't reach here couldn't reach
     *  the Tasks/Notes list either. */
    @Test
    fun displayedEntriesRespectFiltersWhenSortedByDragAndDrop() {
        val openTask = IcalEntry.newTask().copy(summary = "open", categories = listOf("work"))
        val doneTask = IcalEntry.newTask().copy(summary = "done", percentComplete = 100L)
        val otherCategoryTask = IcalEntry.newTask().copy(summary = "other", categories = listOf("home"))

        val state = ListState(
            icalEntries = listOf(openTask, doneTask, otherCategoryTask),
            spectacledVariant = SpectacledVariant.TASKS,
            listSortedBy = ListSortedBy.DRAGANDDROP
        )

        assertEquals(
            setOf(openTask.uid, doneTask.uid, otherCategoryTask.uid),
            state.recompute().displayedEntries.map { it.uid }.toSet()
        )

        val hidingCompleted = state.copy(
            listFilterCriteria = ListFilterCriteria(hideCompletedTasks = true)
        ).recompute()
        assertEquals(
            setOf(openTask.uid, otherCategoryTask.uid),
            hidingCompleted.displayedEntries.map { it.uid }.toSet()
        )

        val filteredByCategory = state.copy(
            listFilterCriteria = ListFilterCriteria(searchCategory = "work")
        ).recompute()
        assertEquals(listOf(openTask.uid), filteredByCategory.displayedEntries.map { it.uid })
    }

    @Test
    fun entriesWithoutOrderNoSortLastUnderDragAndDrop() {
        val unordered = IcalEntry.newTask().copy(summary = "never dragged", orderNo = null)
        val ordered = IcalEntry.newTask().copy(summary = "dragged", orderNo = 1L)

        val state = ListState(
            icalEntries = listOf(unordered, ordered),
            spectacledVariant = SpectacledVariant.TASKS,
            listSortedBy = ListSortedBy.DRAGANDDROP
        ).recompute()

        assertEquals(listOf(ordered.uid, unordered.uid), state.displayedEntries.map { it.uid })
    }
}
