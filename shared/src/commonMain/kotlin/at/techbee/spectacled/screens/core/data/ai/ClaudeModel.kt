package at.techbee.spectacled.screens.core.data.ai

/**
 * The Claude models offered in the settings selector. [id] is the exact Anthropic model string sent
 * on the API request; [displayName] is the human-facing label (a proper noun, not translated).
 *
 * Ordered cheapest/fastest first. [DEFAULT] matches the value the feature shipped with.
 */
enum class ClaudeModel(val id: String, val displayName: String) {
    HAIKU_4_5("claude-haiku-4-5", "Claude Haiku 4.5"),
    SONNET_4_6("claude-sonnet-4-6", "Claude Sonnet 4.6"),
    SONNET_5("claude-sonnet-5", "Claude Sonnet 5"),
    OPUS_4_6("claude-opus-4-6", "Claude Opus 4.6"),
    OPUS_5("claude-opus-5", "Claude Opus 5");

    companion object {
        val DEFAULT = SONNET_4_6

        /** Resolves a stored model id back to an entry, falling back to [DEFAULT] for display. */
        fun fromId(id: String?): ClaudeModel = entries.find { it.id == id } ?: DEFAULT
    }
}
