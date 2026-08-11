package at.techbee.spectacled.screens.core.data.ai

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.parseIcsDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Transport-agnostic DTOs for AI-derived entries. Named generically (not Claude-specific) on
 * purpose so future AI calls can reuse them; the concrete API request lives in the Claude data
 * source ([at.techbee.spectacled.screens.core.data.claude.KtorRemoteClaudeDataSource]).
 *
 * A parent [AiDerivedEntryDto] becomes an [IcalEntry] whose kind is chosen by [AiDerivedEntryDto.type]
 * (note / journal / task). Subtasks are always tasks (VTODO) linked to their parent via
 * `parentUid` + `relType = "PARENT"` - the same model the rest of the app uses.
 */
@Serializable
data class AiDerivedSubtaskDto(
    @SerialName("summary") val summary: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("dtstart") val dtstart: String? = null,
    @SerialName("due") val due: String? = null,
    @SerialName("categories") val categories: List<String>? = null,
)

@Serializable
data class AiDerivedEntryDto(
    @SerialName("type") val type: String? = null,   // "note" | "journal" | "task"
    @SerialName("summary") val summary: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("dtstart") val dtstart: String? = null,     // journal / task
    @SerialName("due") val due: String? = null,             // task
    @SerialName("categories") val categories: List<String>? = null,
    @SerialName("subtasks") val subtasks: List<AiDerivedSubtaskDto>? = null,
)

@Serializable
data class AiDerivedEntryListDto(
    @SerialName("entries") val entries: List<AiDerivedEntryDto> = emptyList()
)

/** Outcome of an AI "derive entries from text" request. */
sealed class AiDeriveEntriesResult {
    data class Success(val entries: List<AiDerivedEntryDto>) : AiDeriveEntriesResult()
    data class Failed(val message: String, val details: String? = null) : AiDeriveEntriesResult()
}

/**
 * A per-generation category applied to every entry (parent and subtasks) created from one AI run,
 * so the user can filter, delete and regenerate a batch. Human-readable timestamp plus a short
 * random suffix to keep two runs within the same minute distinct.
 */
fun newAiBatchCategory(): String {
    val timestamp = formatIcsDateTime(IcsDateTime.now())?.first ?: IcsDateTime.now().toString()
    val suffix = IcalEntry.getRandomUUID().take(4)
    return "🤖 AI $timestamp #$suffix"
}

/**
 * Maps a parent DTO to an [IcalEntry]. The `type` discriminator is what guarantees a parent is
 * always a note, journal or task - anything else is rejected (returns null so the caller skips it),
 * so no unexpected calendar component is ever persisted.
 *
 * [batchCategory] is appended to the entry's categories (see [newAiBatchCategory]).
 */
fun AiDerivedEntryDto.toParentIcalEntry(calendarId: Long, batchCategory: String): IcalEntry? {
    val base = when (type?.trim()?.lowercase()) {
        "note" -> IcalEntry.newNote()
        "journal" -> IcalEntry.newJournal()
        "task" -> IcalEntry.newTask()
        else -> return null
    }
    return base.copy(
        calendarId = calendarId,
        summary = summary,
        description = description,
        // Notes carry no date; journals/tasks take dtstart (journal falls back to newJournal()'s
        // default "now"); only tasks carry a due date.
        dtStart = if (base.isNote()) null else parseIcsDateTime(dtstart) ?: base.dtStart,
        due = if (base.isTask()) parseIcsDateTime(due) else null,
        categories = (categories ?: emptyList()) + batchCategory,
    )
}

/** Maps a subtask DTO to a VTODO [IcalEntry] linked to [parent]. */
fun AiDerivedSubtaskDto.toSubtaskIcalEntry(parent: IcalEntry, batchCategory: String): IcalEntry =
    IcalEntry.newTask().copy(
        calendarId = parent.calendarId,
        parentUid = parent.uid,
        relType = "PARENT",
        summary = summary,
        description = description,
        dtStart = parseIcsDateTime(dtstart),
        due = parseIcsDateTime(due),
        categories = (categories ?: emptyList()) + batchCategory,
    )
