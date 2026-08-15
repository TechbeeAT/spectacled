package at.techbee.spectacled.screens.core.data.ai

/**
 * Which backend fulfils the "derive entries from text" AI feature.
 *
 * [CLAUDE] talks to the hosted Anthropic API with the user's own API key.
 * [OPENAI_COMPATIBLE] talks to any server exposing the OpenAI `/v1/chat/completions` endpoint -
 * an open-source model the user runs themselves (Ollama, LM Studio, llama.cpp server, LocalAI, ...).
 */
enum class AiProvider {
    CLAUDE,
    OPENAI_COMPATIBLE;

    companion object {
        fun fromString(value: String?): AiProvider = entries.find { it.name == value } ?: CLAUDE
    }
}
