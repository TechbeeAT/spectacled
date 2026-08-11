package at.techbee.spectacled.screens.core.data.ai

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
    fun note_mapsToJournalComponentWithoutDates() {
        val note = AiDerivedEntryDto(
            type = "note",
            summary = "A thought",
            description = "Some free-floating idea",
            due = "20260814",           // must be ignored for notes
            categories = listOf("ideas"),
        ).toParentIcalEntry(calendarId, batchCategory)

        assertNotNull(note)
        assertEquals(CalendarComponent.VJOURNAL, note.calendarComponent)
        assertTrue(note.isNote())
        assertNull(note.dtStart)
        assertNull(note.due)
        assertEquals(calendarId, note.calendarId)
        assertTrue(note.categories.contains("ideas"))
        assertTrue(note.categories.contains(batchCategory))
    }

    @Test
    fun journal_isAlwaysDated() {
        // No dtstart supplied -> falls back to newJournal()'s default "now", so it stays a journal.
        val journal = AiDerivedEntryDto(
            type = "journal",
            summary = "Today",
            description = "What happened today",
        ).toParentIcalEntry(calendarId, batchCategory)

        assertNotNull(journal)
        assertEquals(CalendarComponent.VJOURNAL, journal.calendarComponent)
        assertTrue(journal.isJournal())
        assertNotNull(journal.dtStart)
        assertTrue(journal.categories.contains(batchCategory))
    }

    @Test
    fun task_mapsToTodoWithDueAndNoParent() {
        val task = AiDerivedEntryDto(
            type = "task",
            summary = "Pay bills",
            due = "20260814",
        ).toParentIcalEntry(calendarId, batchCategory)

        assertNotNull(task)
        assertEquals(CalendarComponent.VTODO, task.calendarComponent)
        assertTrue(task.isTask())
        assertNull(task.parentUid)          // a parent is not itself a subtask
        assertNotNull(task.due)
        assertTrue(task.categories.contains(batchCategory))
    }

    @Test
    fun unknownType_isRejected() {
        assertNull(
            AiDerivedEntryDto(type = "event", summary = "Meeting")
                .toParentIcalEntry(calendarId, batchCategory)
        )
        assertNull(
            AiDerivedEntryDto(type = null, summary = "No type")
                .toParentIcalEntry(calendarId, batchCategory)
        )
    }

    @Test
    fun type_isCaseAndWhitespaceInsensitive() {
        val task = AiDerivedEntryDto(type = "  Task ", summary = "Trim me")
            .toParentIcalEntry(calendarId, batchCategory)
        assertNotNull(task)
        assertTrue(task.isTask())
    }

    @Test
    fun subtask_isTodoLinkedToParentWithDates() {
        val parent = AiDerivedEntryDto(type = "task", summary = "Friday todos")
            .toParentIcalEntry(calendarId, batchCategory)
        assertNotNull(parent)

        val subtask = AiDerivedSubtaskDto(
            summary = "Water plants",
            dtstart = "20260814",
            due = "20260814",
            categories = listOf("home"),
        ).toSubtaskIcalEntry(parent, batchCategory)

        assertEquals(CalendarComponent.VTODO, subtask.calendarComponent)
        assertEquals(parent.uid, subtask.parentUid)
        assertEquals("PARENT", subtask.relType)
        assertEquals(parent.calendarId, subtask.calendarId)
        assertTrue(subtask.isSubtask())
        assertNotNull(subtask.dtStart)
        assertNotNull(subtask.due)
        assertTrue(subtask.categories.contains("home"))
        assertTrue(subtask.categories.contains(batchCategory))
    }

    @Test
    fun listDeserializes_fromClaudeStyleJson() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val payload = """
            {
              "entries": [
                {
                  "type": "task",
                  "summary": "Friday todos",
                  "subtasks": [
                    { "summary": "Clean car" },
                    { "summary": "Water plants" }
                  ]
                },
                { "type": "note", "summary": "Remember the milk" }
              ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<AiDerivedEntryListDto>(payload)
        assertEquals(2, parsed.entries.size)
        assertEquals("task", parsed.entries[0].type)
        assertEquals(2, parsed.entries[0].subtasks?.size)
        assertEquals("note", parsed.entries[1].type)
    }
}
