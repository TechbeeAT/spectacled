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
    fun perCalendarMapRoundTripsWithoutSearchQuery() {
        val stored = mapOf(
            1L to ListFilterCriteria(
                searchQuery = "not stored",
                searchCategories = listOf("with,comma", "back\\slash"),
                filterStatus = listOf(Status.IN_PROCESS, null),
                hideCompletedTasks = true
            ),
            42L to ListFilterCriteria()
        )

        val json = ListFilterCriteria.mapperJson.encodeToString(stored)
        val restored = ListFilterCriteria.mapperJson.decodeFromString<Map<Long, ListFilterCriteria>>(json)

        assertFalse(json.contains("not stored"))
        assertEquals(stored.mapValues { it.value.copy(searchQuery = null) }, restored)
    }

    @Test
    fun unknownKeysAreIgnored() {
        val restored = ListFilterCriteria.mapperJson.decodeFromString<ListFilterCriteria>(
            """{"searchCategories":["Work"],"addedInNewerVersion":true}"""
        )
        assertEquals(ListFilterCriteria(searchCategories = listOf("Work")), restored)
    }
}
