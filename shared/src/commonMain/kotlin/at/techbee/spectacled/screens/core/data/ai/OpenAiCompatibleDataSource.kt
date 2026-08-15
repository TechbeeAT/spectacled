package at.techbee.spectacled.screens.core.data.ai

import at.techbee.spectacled.SpectacledVariant
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Derives entries via any server that speaks the OpenAI `POST /v1/chat/completions` API - i.e. an
 * open-source model the user runs themselves (Ollama, LM Studio, llama.cpp server, LocalAI, ...).
 * Fits the app's self-hosting ethos and works on every platform since it is plain HTTP over the
 * shared [HttpClient] (including its proxy interceptor, which covers Web/CORS).
 *
 * [baseUrl] is the server root, e.g. `http://localhost:11434`; `/v1/chat/completions` is appended.
 * [model] is the served model name, e.g. `llama3.2`. [apiKey] is optional - sent as a bearer token
 * only when non-blank (Ollama needs none; some servers do).
 */
class OpenAiCompatibleDataSource(
    private val client: HttpClient,
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String?,
) : AiDeriveEntriesDataSource {

    override suspend fun deriveEntries(
        rawText: String,
        variant: SpectacledVariant,
        createSubtasks: Boolean,
        existingCategories: List<String>,
    ): AiDeriveEntriesResult {

        val prompt = buildDeriveEntriesPrompt(rawText, variant, createSubtasks, existingCategories)
        val endpoint = baseUrl.trimEnd('/') + "/v1/chat/completions"

        return try {
            val response = client.post(endpoint) {
                contentType(ContentType.Application.Json)
                if (!apiKey.isNullOrBlank()) header(HttpHeaders.Authorization, "Bearer $apiKey")
                setBody(buildJsonObject {
                    put("model", model)
                    put("stream", false)
                    // Ask servers that support it to constrain output to a JSON object. Servers that
                    // ignore the hint still work: parseDerivedEntries() extracts the JSON defensively.
                    putJsonObject("response_format") { put("type", "json_object") }
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        }
                    }
                })
            }.body<OpenAiChatResponseDto>()

            val text = response.choices.firstOrNull()?.message?.content
                ?: return AiDeriveEntriesResult.Failed("AI response contained no text")

            AiDeriveEntriesResult.Success(parseDerivedEntries(text))

        } catch (e: Exception) {
            Napier.e("AI derive-entries request (OpenAI-compatible) failed", e)
            AiDeriveEntriesResult.Failed(
                message = "Fetching AI response failed",
                details = e.message
            )
        }
    }
}

@Serializable
data class OpenAiChatResponseDto(
    @SerialName("choices") val choices: List<OpenAiChoiceDto> = emptyList()
)

@Serializable
data class OpenAiChoiceDto(
    @SerialName("message") val message: OpenAiMessageDto? = null
)

@Serializable
data class OpenAiMessageDto(
    @SerialName("content") val content: String? = null
)
