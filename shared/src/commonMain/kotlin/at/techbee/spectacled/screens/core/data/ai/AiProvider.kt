package at.techbee.spectacled.screens.core.data.ai

import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.ai_provider_claude
import spectacled.shared.generated.resources.ai_provider_none
import spectacled.shared.generated.resources.ai_provider_openai

/**
 * Which backend fulfils the "derive entries from text" AI feature.
 *
 * [CLAUDE] talks to the hosted Anthropic API with the user's own API key.
 * [OPENAI_COMPATIBLE] talks to any server exposing the OpenAI `/v1/chat/completions` endpoint -
 * an open-source model the user runs themselves (Ollama, LM Studio, llama.cpp server, LocalAI, ...).
 */
enum class AiProvider(val providerNameRes: StringResource) {
    NONE(Res.string.ai_provider_none),
    CLAUDE(Res.string.ai_provider_claude),
    OPENAI_COMPATIBLE(Res.string.ai_provider_openai);

    companion object {
        fun fromString(value: String?): AiProvider = entries.find { it.name == value } ?: NONE
    }
}
