package at.techbee.spectacled.screens.core.data.claude

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ai.AiDeriveEntriesResult
import at.techbee.spectacled.screens.core.data.ai.AiDerivedEntryListDto
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1/messages"

private val aiJson = Json { ignoreUnknownKeys = true }

sealed class ClaudeRemoteResponseResult {
    data class Success(val icalEntry: IcalEntry) : ClaudeRemoteResponseResult()
    data class Failed(val message: String, val details: String? = null) : ClaudeRemoteResponseResult()
}

class KtorRemoteClaudeDataSource(
    val client: HttpClient,
    val claudeUserApiKey: String
) {

    suspend fun applyAiMetadata(icalEntry: IcalEntry): ClaudeRemoteResponseResult {

        val dtStartPromptPart = if (icalEntry.isJournal()) {
            """
                "dtstart": "The date or datetime of the journal entry RFC-5545 compliant if mentioned, otherwise null", 
                """
        } else if (icalEntry.isTask()) {
            """
                "dtstart": "The start date or start datetime of the task entry RFC-5545 compliant if mentioned, otherwise null. Make sure that the dtstart and due date/datetime have the same format, date or datetime.", 
                """
        } else
            ""

        val duePromptPart = if (icalEntry.isTask()) {
            """
                "due": "The due date or datetime of the task entry RFC-5545 compliant if mentioned, otherwise null. Make sure that the dtstart and due date/datetime have the same format, date or datetime.", 
                """
        } else
            ""

        val prompt = """
        You are a structured data extractor. The user will give you a free-text journal entry.
        Extract the following fields and return ONLY valid JSON, no markdown, no explanation:
        
        {
          "summary": "A short one-line title for the entry",
          "description": "The full cleaned-up text of the entry",
          $dtStartPromptPart
          $duePromptPart
          "location": "Physical location if mentioned, otherwise null",
          "categories": ["list", "of", "topic", "tags"]
        }
        
        Now is ${formatIcsDateTime(IcsDateTime.now())!!.first}
                
        Raw text:
        ${icalEntry.summary}
        ${icalEntry.description}
    """.trimIndent()

        try {
            val response = client.post(ANTHROPIC_BASE_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", claudeUserApiKey)
                header("anthropic-version", "2023-06-01")
                setBody(buildJsonObject {
                    put("model", "claude-sonnet-4-6")
                    put("max_tokens", 1000)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    }
                })
            }.body<ClaudeResponseDto>()

            return ClaudeRemoteResponseResult.Success(response.applyClaudeResponse(icalEntry))

        } catch (e: Exception) {
            Napier.e("AI metadata request failed", e)
            return ClaudeRemoteResponseResult.Failed(
                message = "Fetching AI response failed",
                details = e.message
            )
        }
    }

    /**
     * Derives a list of entries with subtasks from free [rawText]. Returns the parsed,
     * transport-agnostic DTOs; mapping to [IcalEntry] + persistence is the caller's job (it owns the
     * calendar id, the per-generation batch category, and the kind of the top-level entries).
     *
     * [variant] tells the model what the top-level entries are for the current list (its lowercase
     * enum name - "notes" / "journals" / "tasks"), so the model never has to choose a type.
     * [createSubtasks] mirrors the user's toggle: when false the model is asked for a flat list.
     * (The mapper enforces this regardless, so this is only a token/quality hint.)
     * [existingCategories] are the categories already used in the system; the model is asked to
     * prefer reusing them over inventing near-duplicates.
     *
     * Reliability note: this uses the same "ask for JSON, then parse" approach as [applyAiMetadata]
     * for consistency with the existing integration. Because the shape is recursive it cannot use
     * structured outputs (their JSON schemas can't be recursive); keep this prompt-based path if you
     * want nested subtasks.
     */
    suspend fun deriveEntries(
        rawText: String,
        variant: SpectacledVariant,
        createSubtasks: Boolean,
        existingCategories: List<String> = emptyList(),
    ): AiDeriveEntriesResult {

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

        val prompt = """
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

        return try {
            val response = client.post(ANTHROPIC_BASE_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", claudeUserApiKey)
                header("anthropic-version", "2023-06-01")
                setBody(buildJsonObject {
                    put("model", "claude-sonnet-4-6")
                    put("max_tokens", 4096)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    }
                })
            }.body<ClaudeResponseDto>()

            val text = response.content.firstOrNull { it.type == "text" }?.text
                ?: return AiDeriveEntriesResult.Failed("AI response contained no text")

            val parsed = aiJson.decodeFromString<AiDerivedEntryListDto>(text)
            AiDeriveEntriesResult.Success(parsed.entries)

        } catch (e: Exception) {
            Napier.e("AI derive-entries request failed", e)
            AiDeriveEntriesResult.Failed(
                message = "Fetching AI response failed",
                details = e.message
            )
        }
    }
}