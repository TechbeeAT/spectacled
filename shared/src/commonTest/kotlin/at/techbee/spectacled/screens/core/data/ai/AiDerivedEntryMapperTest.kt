package at.techbee.spectacled.screens.core.data.ai

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiDerivedEntryMapperTest {

    private val batchCategory = "🤖 AI test-batch"
    private val calendarId = 42L

    @Test
    fun notesVariant_topLevelIsNoteWithoutDates() {
        val entries = AiDerivedEntryDto(
            summary = "A thought",
            description = "Some free-floating idea",
            due = "20260814",           // must be ignored for notes
            categories = listOf("ideas"),
        ).toIcalEntries(calendarId, batchCategory, SpectacledVariant.NOTES)

        assertEquals(1, entries.size)
        val note = entries.single()
        assertEquals(CalendarComponent.VJOURNAL, note.calendarComponent)
        assertTrue(note.isNote())
        assertNull(note.dtStart)
        assertNull(note.due)
        assertNull(note.parentUid)
        assertEquals(calendarId, note.calendarId)
        assertTrue(note.categories.contains("ideas"))
        assertTrue(note.categories.contains(batchCategory))
    }

    @Test
    fun journalsVariant_topLevelIsAlwaysDated() {
        // No dtstart supplied -> falls back to newJournal()'s default "now", so it stays a journal.
        val journal = AiDerivedEntryDto(
            summary = "Today",
            description = "What happened today",
        ).toIcalEntries(calendarId, batchCategory, SpectacledVariant.JOURNALS).single()

        assertEquals(CalendarComponent.VJOURNAL, journal.calendarComponent)
        assertTrue(journal.isJournal())
        assertNotNull(journal.dtStart)
        assertTrue(journal.categories.contains(batchCategory))
    }

    @Test
    fun tasksVariant_topLevelIsTodoWithDueAndNoParent() {
        val task = AiDerivedEntryDto(
            summary = "Pay bills",
            due = "20260814",
        ).toIcalEntries(calendarId, batchCategory, SpectacledVariant.TASKS).single()

        assertEquals(CalendarComponent.VTODO, task.calendarComponent)
        assertTrue(task.isTask())
        assertNull(task.parentUid)          // a parent is not itself a subtask
        assertNotNull(task.due)
        assertTrue(task.categories.contains(batchCategory))
    }

    @Test
    fun contentlessEntry_isSkipped() {
        assertTrue(
            AiDerivedEntryDto(summary = "   ", description = null)
                .toIcalEntries(calendarId, batchCategory, SpectacledVariant.TASKS)
                .isEmpty()
        )
    }

    @Test
    fun descendantsAreTasks_linkedToImmediateParent_atAnyDepth() {
        // Even in the NOTES variant the top-level is a note, but every descendant is a task (VTODO).
        val entries = AiDerivedEntryDto(
            summary = "Friday todos",
            subtasks = listOf(
                AiDerivedEntryDto(
                    summary = "Clean car",
                    dtstart = "20260814",
                    due = "20260814",
                    categories = listOf("home"),
                    subtasks = listOf(
                        AiDerivedEntryDto(summary = "Buy sponge"),   // sub-subtask
                    ),
                ),
                AiDerivedEntryDto(summary = "Water plants"),
            ),
        ).toIcalEntries(calendarId, batchCategory, SpectacledVariant.NOTES)

        // parent + 2 subtasks + 1 sub-subtask, parent-first order
        assertEquals(4, entries.size)

        val parent = entries[0]
        assertTrue(parent.isNote())
        assertNull(parent.parentUid)

        val cleanCar = entries[1]
        assertEquals(CalendarComponent.VTODO, cleanCar.calendarComponent)
        assertEquals(parent.uid, cleanCar.parentUid)
        assertEquals("PARENT", cleanCar.relType)
        assertNotNull(cleanCar.dtStart)
        assertNotNull(cleanCar.due)
        assertTrue(cleanCar.categories.contains("home"))

        val buySponge = entries[2]
        assertEquals(CalendarComponent.VTODO, buySponge.calendarComponent)
        assertEquals(cleanCar.uid, buySponge.parentUid)   // linked to its immediate parent, not the root
        assertEquals("PARENT", buySponge.relType)

        val waterPlants = entries[3]
        assertEquals(CalendarComponent.VTODO, waterPlants.calendarComponent)
        assertEquals(parent.uid, waterPlants.parentUid)

        // batch category on every node
        assertTrue(entries.all { it.categories.contains(batchCategory) })
    }

    @Test
    fun url_isParsedOnParentAndSubtask_blankBecomesNull() {
        val entries = AiDerivedEntryDto(
            summary = "Read this",
            url = "https://example.com/article",
            subtasks = listOf(
                AiDerivedEntryDto(summary = "Reference", url = "https://example.org/ref"),
                AiDerivedEntryDto(summary = "No link", url = "   "),   // blank -> null
            ),
        ).toIcalEntries(calendarId, batchCategory, SpectacledVariant.TASKS)

        val parent = entries[0]
        assertNotNull(parent.url)
        assertTrue(parent.url.toString().contains("example.com"))

        val withLink = entries[1]
        assertNotNull(withLink.url)
        assertTrue(withLink.url.toString().contains("example.org"))

        val noLink = entries[2]
        assertNull(noLink.url)
    }

    @Test
    fun url_absent_isNull() {
        val note = AiDerivedEntryDto(summary = "No url here")
            .toIcalEntries(calendarId, batchCategory, SpectacledVariant.NOTES)
            .single()
        assertNull(note.url)
    }

    @Test
    fun includeSubtasksFalse_dropsAllSubtasks() {
        val entries = AiDerivedEntryDto(
            summary = "Friday todos",
            subtasks = listOf(
                AiDerivedEntryDto(summary = "Clean car"),
                AiDerivedEntryDto(summary = "Water plants"),
            ),
        ).toIcalEntries(calendarId, batchCategory, SpectacledVariant.TASKS, includeSubtasks = false)

        assertEquals(1, entries.size)
        val parent = entries.single()
        assertTrue(parent.isTask())
        assertNull(parent.parentUid)
        assertTrue(parent.categories.contains(batchCategory))
    }

    @Test
    fun batchCategory_startsWithSharedPrefix() {
        assertTrue(newAiBatchCategory().startsWith(AI_BATCH_CATEGORY_PREFIX))
    }

    @Test
    fun listDeserializes_fromRecursiveJson() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val payload = """
            {
              "entries": [
                {
                  "summary": "Friday todos",
                  "subtasks": [
                    { "summary": "Clean car", "subtasks": [ { "summary": "Buy sponge" } ] },
                    { "summary": "Water plants" }
                  ]
                },
                { "summary": "Remember the milk" }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<AiDerivedEntryListDto>(payload)
        assertEquals(2, parsed.entries.size)
        assertEquals(2, parsed.entries[0].subtasks?.size)
        assertEquals("Buy sponge", parsed.entries[0].subtasks?.get(0)?.subtasks?.get(0)?.summary)
        assertNull(parsed.entries[1].subtasks)
    }
}
