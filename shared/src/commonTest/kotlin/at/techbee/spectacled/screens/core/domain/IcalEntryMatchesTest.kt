package at.techbee.spectacled.screens.core.domain

import at.techbee.spectacled.screens.list.presentation.datastructures.ListFilterCriteria
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IcalEntryMatchesTest {

    private val work = IcalEntry.newTask().copy(categories = listOf("Work"))
    private val home = IcalEntry.newTask().copy(categories = listOf("home", "garden"))
    private val uncategorized = IcalEntry.newTask()

    @Test
    fun emptyCriteriaMatchEverything() {
        val criteria = ListFilterCriteria()
        assertTrue(work.matches(criteria))
        assertTrue(uncategorized.matches(criteria))
    }

    @Test
    fun anySelectedCategoryMatches() {
        val criteria = ListFilterCriteria(searchCategories = listOf("Work", "garden"))
        assertTrue(work.matches(criteria))
        assertTrue(home.matches(criteria))
        assertFalse(uncategorized.matches(criteria))
    }

    @Test
    fun categoryMatchIgnoresCase() {
        assertTrue(work.matches(ListFilterCriteria(searchCategories = listOf("work"))))
        assertTrue(home.matches(ListFilterCriteria(searchCategories = listOf("HOME"))))
        assertFalse(work.matches(ListFilterCriteria(searchCategories = listOf("home"))))
    }

    @Test
    fun anySelectedStatusMatches() {
        val criteria = ListFilterCriteria(filterStatus = listOf(Status.IN_PROCESS, Status.CANCELLED))
        assertTrue(IcalEntry.newTask().copy(status = Status.IN_PROCESS).matches(criteria))
        assertTrue(IcalEntry.newTask().copy(status = Status.CANCELLED).matches(criteria))
        assertFalse(IcalEntry.newTask().copy(status = Status.NEEDS_ACTION).matches(criteria))
        assertFalse(IcalEntry.newTask().matches(criteria))
    }

    @Test
    fun nullStatusSelectsEntriesWithoutStatus() {
        val noStatusOnly = ListFilterCriteria(filterStatus = listOf(null))
        assertTrue(IcalEntry.newJournal().matches(noStatusOnly))
        assertFalse(IcalEntry.newJournal().copy(status = Status.FINAL).matches(noStatusOnly))

        val noStatusOrDraft = ListFilterCriteria(filterStatus = listOf(null, Status.DRAFT))
        assertTrue(IcalEntry.newJournal().matches(noStatusOrDraft))
        assertTrue(IcalEntry.newJournal().copy(status = Status.DRAFT).matches(noStatusOrDraft))
        assertFalse(IcalEntry.newJournal().copy(status = Status.FINAL).matches(noStatusOrDraft))
    }

    @Test
    fun categoryAndStatusMustBothMatch() {
        val criteria = ListFilterCriteria(searchCategories = listOf("work"), filterStatus = listOf(Status.IN_PROCESS))
        assertTrue(work.copy(status = Status.IN_PROCESS).matches(criteria))
        assertFalse(work.copy(status = Status.NEEDS_ACTION).matches(criteria))
        assertFalse(home.copy(status = Status.IN_PROCESS).matches(criteria))
    }
}
