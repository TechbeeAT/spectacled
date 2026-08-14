package at.techbee.spectacled.screens.core.data.ai

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.parseIcsDateTime
import io.ktor.http.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Transport-agnostic DTO for AI-derived entries. Named generically (not Claude-specific) on purpose
 * so future AI calls can reuse it; the concrete API request lives in the Claude data source
 * ([at.techbee.spectacled.screens.core.data.claude.KtorRemoteClaudeDataSource]).
 *
 * The DTO is recursive - a subtask is just another [AiDerivedEntryDto] - which mirrors the app's own
 * arbitrary-depth `parentUid` model. There is deliberately no "type" field: the kind of the
 * top-level entry (note / journal / task) is decided by the list the user is on
 * ([SpectacledVariant]), exactly like the per-variant "add" FAB. Every descendant is always a task
 * (VTODO), regardless of the top-level kind.
 */
@Serializable
data class AiDerivedEntryDto(
    @SerialName("summary") val summary: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("dtstart") val dtstart: String? = null,
    @SerialName("due") val due: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("categories") val categories: List<String>? = null,
    @SerialName("subtasks") val subtasks: List<AiDerivedEntryDto>? = null,
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

/** Prefix shared by every per-generation AI batch category (see [newAiBatchCategory]). */
const val AI_BATCH_CATEGORY_PREFIX = "🤖 AI"

/**
 * A per-generation category applied to every entry (parent and subtasks) created from one AI run,
 * so the user can filter, delete and regenerate a batch. Human-readable timestamp plus a short
 * random suffix to keep two runs within the same minute distinct.
 */
fun newAiBatchCategory(): String {
    val timestamp = formatIcsDateTime(IcsDateTime.now())?.first ?: IcsDateTime.now().toString()
    val suffix = IcalEntry.getRandomUUID().take(4)
    return "$AI_BATCH_CATEGORY_PREFIX $timestamp #$suffix"
}

/**
 * Flattens a derived entry (and, when [includeSubtasks] is true, its whole subtask subtree) into a
 * parent-first list of [IcalEntry]. The top-level entry's kind is taken from [variant] (so the
 * "parent is a note/journal/task" guarantee is structural); every descendant is a task (VTODO)
 * linked to its immediate parent via `parentUid` + `relType = "PARENT"`. Insert the returned list in
 * order. With [includeSubtasks] false, only the top-level entry is produced, whatever the model
 * returned.
 */
fun AiDerivedEntryDto.toIcalEntries(
    calendarId: Long,
    batchCategory: String,
    variant: SpectacledVariant,
    includeSubtasks: Boolean = true,
): List<IcalEntry> {
    val base = when (variant) {
        SpectacledVariant.NOTES -> IcalEntry.newNote()
        SpectacledVariant.JOURNALS -> IcalEntry.newJournal()
        SpectacledVariant.TASKS -> IcalEntry.newTask()
    }
    return flatten(base, parentUid = null, calendarId = calendarId, batchCategory = batchCategory, includeSubtasks = includeSubtasks)
}

private fun AiDerivedEntryDto.flatten(
    base: IcalEntry,
    parentUid: String?,
    calendarId: Long,
    batchCategory: String,
    includeSubtasks: Boolean,
): List<IcalEntry> {
    val entry = base.copy(
        calendarId = calendarId,
        summary = summary,
        description = description,
        // Notes carry no date; journals/tasks take dtstart (journal falls back to newJournal()'s
        // default "now"); only tasks carry a due date.
        dtStart = if (base.isNote()) null else parseIcsDateTime(dtstart) ?: base.dtStart,
        due = if (base.isTask()) parseIcsDateTime(due) else null,
        // AI output is untrusted; ktor Url(String) can throw on malformed input, so parse defensively.
        url = url?.takeIf { it.isNotBlank() }?.let { runCatching { Url(it) }.getOrNull() },
        categories = (categories ?: emptyList()) + batchCategory,
        parentUid = parentUid,
        relType = if (parentUid != null) "PARENT" else null,
    )

    // Skip a content-less node (and its subtree) so we never persist empty or orphaned entries.
    if (entry.summary.isNullOrBlank() && entry.description.isNullOrBlank())
        return emptyList()

    // Descendants are always tasks (VTODO), regardless of the top-level kind. Skipped entirely when
    // the user turned subtask creation off.
    val children = if (includeSubtasks)
        subtasks.orEmpty().flatMap { it.flatten(IcalEntry.newTask(), entry.uid, calendarId, batchCategory, true) }
    else
        emptyList()

    return listOf(entry) + children
}
