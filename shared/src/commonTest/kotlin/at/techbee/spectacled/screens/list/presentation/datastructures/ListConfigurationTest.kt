package at.techbee.spectacled.screens.list.presentation.datastructures

import at.techbee.spectacled.screens.core.domain.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListConfigurationTest {

    @Test
    fun toggleCategoryAddsAndRemovesIgnoringCase() {
        val selected = ListConfiguration().toggleCategory("Work")
        assertEquals(listOf("Work"), selected.searchCategories)
        assertTrue(selected.isCategorySelected("work"))

        assertEquals(emptyList(), selected.toggleCategory("WORK").searchCategories)
    }

    @Test
    fun toggleStatusAddsAndRemovesIncludingNoStatus() {
        val selected = ListConfiguration().toggleStatus(Status.DRAFT).toggleStatus(null)
        assertEquals(listOf(Status.DRAFT, null), selected.filterStatus)
        assertEquals(listOf<Status?>(Status.DRAFT), selected.toggleStatus(null).filterStatus)
        assertEquals(listOf(null), selected.toggleStatus(Status.DRAFT).filterStatus)
    }

    @Test
    fun anyFilterActiveForMultiselectLists() {
        assertFalse(ListConfiguration().anyFilterActive())
        assertTrue(ListConfiguration(searchCategories = listOf("a")).anyFilterActive())
        assertTrue(ListConfiguration(filterStatus = listOf(null)).anyFilterActive())
    }

    @Test
    fun escapedJoinRoundTrips() {
        val values = listOf("plain", "with,comma", "back\\slash", "trailing\\", ",", "", "a\\,b")
        assertEquals(values, ListConfiguration.splitEscaped(ListConfiguration.joinEscaped(values)))
    }

    @Test
    fun escapedJoinEscapesSeparatorAndEscape() {
        assertEquals("a\\,b,c\\\\d", ListConfiguration.joinEscaped(listOf("a,b", "c\\d")))
    }

    @Test
    fun splitOfLegacySingleValueIsUnchanged() {
        assertEquals(listOf("Category 1"), ListConfiguration.splitEscaped("Category 1"))
        assertEquals(listOf("IN_PROCESS"), ListConfiguration.splitEscaped("IN_PROCESS"))
    }

    @Test
    fun splitKeepsDanglingEscape() {
        assertEquals(listOf("abc\\"), ListConfiguration.splitEscaped("abc\\"))
    }
}
