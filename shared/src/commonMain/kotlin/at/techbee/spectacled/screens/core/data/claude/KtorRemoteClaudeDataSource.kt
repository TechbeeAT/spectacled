package at.techbee.spectacled.screens.core.data.claude

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.ioDispatcher
import at.techbee.spectacled.screens.core.mapper.ics.formatIcsDateTime
import io.github.aakira.napier.Napier
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

private const val ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1/messages"

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

        // Dispatch the network call (and response parsing) onto ioDispatcher so callers launch on
        // Main without naming a dispatcher.
        return withContext(ioDispatcher) {
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

                ClaudeRemoteResponseResult.Success(response.applyClaudeResponse(icalEntry))

            } catch (e: Exception) {
                Napier.e("AI metadata request failed", e)
                ClaudeRemoteResponseResult.Failed(
                    message = "Fetching AI response failed",
                    details = e.message
                )
            }
        }
    }
}