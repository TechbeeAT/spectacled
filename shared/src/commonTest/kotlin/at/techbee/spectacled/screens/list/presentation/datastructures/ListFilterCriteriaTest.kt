package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.domain.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListFilterCriteriaTest {

    @Test
    fun toggleCategoryAddsAndRemovesIgnoringCase() {
        val selected = ListFilterCriteria().toggleCategory("Work")
        assertEquals(listOf("Work"), selected.searchCategories)
        assertTrue(selected.isCategorySelected("work"))

        assertEquals(emptyList(), selected.toggleCategory("WORK").searchCategories)
    }

    @Test
    fun toggleStatusAddsAndRemovesIncludingNoStatus() {
        val selected = ListFilterCriteria().toggleStatus(Status.DRAFT).toggleStatus(null)
        assertEquals(listOf(Status.DRAFT, null), selected.filterStatus)
        assertEquals(listOf<Status?>(Status.DRAFT), selected.toggleStatus(null).filterStatus)
        assertEquals(listOf(null), selected.toggleStatus(Status.DRAFT).filterStatus)
    }

    @Test
    fun anyFilterActiveForMultiselectLists() {
        assertFalse(ListFilterCriteria().anyFilterActive())
        assertTrue(ListFilterCriteria(searchCategories = listOf("a")).anyFilterActive())
        assertTrue(ListFilterCriteria(filterStatus = listOf(null)).anyFilterActive())
    }

    @Test
    fun escapedJoinRoundTrips() {
        val values = listOf("plain", "with,comma", "back\\slash", "trailing\\", ",", "", "a\\,b")
        assertEquals(values, ListFilterCriteria.splitEscaped(ListFilterCriteria.joinEscaped(values)))
    }

    @Test
    fun escapedJoinEscapesSeparatorAndEscape() {
        assertEquals("a\\,b,c\\\\d", ListFilterCriteria.joinEscaped(listOf("a,b", "c\\d")))
    }

    @Test
    fun splitOfLegacySingleValueIsUnchanged() {
        assertEquals(listOf("Category 1"), ListFilterCriteria.splitEscaped("Category 1"))
        assertEquals(listOf("IN_PROCESS"), ListFilterCriteria.splitEscaped("IN_PROCESS"))
    }

    @Test
    fun splitKeepsDanglingEscape() {
        assertEquals(listOf("abc\\"), ListFilterCriteria.splitEscaped("abc\\"))
    }
}
