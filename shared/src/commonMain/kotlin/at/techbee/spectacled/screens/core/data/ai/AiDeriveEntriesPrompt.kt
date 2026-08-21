package at.techbee.spectacled.screens.core.data.ai

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import kotlinx.serialization.json.Json

private val aiJson = Json { ignoreUnknownKeys = true }

/**
 * Builds the provider-neutral "derive entries from free text" prompt. Shared by every
 * [AiDeriveEntriesDataSource] so the hosted (Claude) and self-hosted (OpenAI-compatible) backends
 * send an identical instruction.
 *
 * [variant] names what the top-level entries are for the current list ("notes" / "journals" /
 * "tasks") so the model never chooses a type. [createSubtasks] mirrors the user's toggle (the
 * mapper enforces it regardless, so this is a quality/token hint). [existingCategories] are asked
 * to be reused over near-duplicates.
 *
 * Reliability note: the shape is recursive, which rules out JSON-schema structured outputs
 * (their schemas can't be recursive); keep this prompt-based path for nested subtasks.
 */
fun buildDeriveEntriesPrompt(
    rawText: String,
    variant: SpectacledVariant,
    createSubtasks: Boolean,
    existingCategories: List<String>,
): String {

    val variantName = variant.name.lowercase()   // "notes" | "journals" | "tasks"

    val subtaskGuidance = if (createSubtasks) {
        """
        Group closely related actionable items under a single parent as subtasks - for example a
        parent "Friday todos" with subtasks "clean car", "water plants". Keep nesting shallow
        (ideally one level of subtasks). Do NOT invent recurrence; a repeating chore list is just
        a parent with subtasks.
        """.trimIndent()
    } else {
        "Do NOT create subtasks; return a flat list of top-level entries only, each with an empty \"subtasks\" array."
    }

    val categoryGuidance = if (existingCategories.isNotEmpty()) {
        """

        The user already uses these categories: ${existingCategories.joinToString(", ")}.
        Prefer reusing an existing category verbatim (same spelling and casing) when it fits the
        topic; only create a new category when none of the existing ones apply.
        """.trimIndent()
    } else {
        ""
    }

    return """
        You are a structured data extractor. The user gives you free text describing things to
        remember and things to do. Split it into a list of entries and return ONLY valid JSON,
        no markdown, no explanation.

        Each top-level entry will be saved as one of the user's $variantName. $subtaskGuidance

        Return JSON of exactly this shape (an entry and a subtask have the same shape; subtasks
        may themselves have subtasks):
        {
          "entries": [
            {
              "summary": "A short one-line title for the entry",
              "description": "The full cleaned-up text of the entry, or null",
              "dtstart": "RFC-5545 compliant date or datetime if mentioned, otherwise null",
              "due": "RFC-5545 compliant date or datetime if mentioned, otherwise null",
              "url": "An http/https URL if one is mentioned, otherwise null",
              "categories": ["list", "of", "topic", "tags"],
              "subtasks": [
                {
                  "summary": "A short one-line title for the subtask",
                  "description": "Details of the subtask, or null",
                  "dtstart": "RFC-5545 compliant date or datetime if mentioned, otherwise null",
                  "due": "RFC-5545 compliant date or datetime if mentioned, otherwise null",
                  "url": "An http/https URL if one is mentioned, otherwise null",
                  "categories": ["tags"],
                  "subtasks": []
                }
              ]
            }
          ]
        }

        Within one entry, make sure dtstart and due use the same format (both date or both
        datetime). Use an empty array for "subtasks" when there are none.
        $categoryGuidance

        Now is ${formatIcsDateTime(IcsDateTime.now())!!.first}

        Raw text:
        $rawText
    """.trimIndent()
}

/**
 * Parses a model's raw text answer into [AiDerivedEntryDto]s. Self-hosted open-source models are
 * less strict than Claude about "return ONLY JSON", so the outermost `{...}` block is extracted
 * first (stripping ```json fences or stray prose) before decoding. Throws on malformed JSON - the
 * caller wraps this in a try/catch and surfaces an [AiDeriveEntriesResult.Failed].
 */
fun parseDerivedEntries(text: String): List<AiDerivedEntryDto> {
    val jsonText = extractOutermostJsonObject(text)
    return aiJson.decodeFromString<AiDerivedEntryListDto>(jsonText).entries
}

private fun extractOutermostJsonObject(text: String): String {
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    return if (start >= 0 && end > start) text.substring(start, end + 1) else text
}
