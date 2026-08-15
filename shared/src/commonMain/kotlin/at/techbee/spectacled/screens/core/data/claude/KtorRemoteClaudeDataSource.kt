package at.techbee.spectacled.screens.core.data.claude

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.ai.AiDeriveEntriesDataSource
import at.techbee.spectacled.screens.core.data.ai.AiDeriveEntriesResult
import at.techbee.spectacled.screens.core.data.ai.buildDeriveEntriesPrompt
import at.techbee.spectacled.screens.core.data.ai.parseDerivedEntries
import io.github.aakira.napier.Napier
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

/**
 * Derives entries via the hosted Anthropic API using the user's own [claudeUserApiKey] and the
 * selected [model] (an Anthropic model id - see
 * [at.techbee.spectacled.screens.core.data.ai.ClaudeModel]). The prompt and response parsing are
 * shared with every other [AiDeriveEntriesDataSource] (see [buildDeriveEntriesPrompt] /
 * [parseDerivedEntries]); only the request transport is Claude-specific.
 */
class KtorRemoteClaudeDataSource(
    val client: HttpClient,
    val claudeUserApiKey: String,
    val model: String,
) : AiDeriveEntriesDataSource {

    override suspend fun deriveEntries(
        rawText: String,
        variant: SpectacledVariant,
        createSubtasks: Boolean,
        existingCategories: List<String>,
    ): AiDeriveEntriesResult {

        val prompt = buildDeriveEntriesPrompt(rawText, variant, createSubtasks, existingCategories)

        return try {
            val response = client.post(ANTHROPIC_BASE_URL) {
                contentType(ContentType.Application.Json)
                header("x-api-key", claudeUserApiKey)
                header("anthropic-version", "2023-06-01")
                setBody(buildJsonObject {
                    put("model", model)
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

            AiDeriveEntriesResult.Success(parseDerivedEntries(text))

        } catch (e: Exception) {
            Napier.e("AI derive-entries request failed", e)
            AiDeriveEntriesResult.Failed(
                message = "Fetching AI response failed",
                details = e.message
            )
        }
    }
}
