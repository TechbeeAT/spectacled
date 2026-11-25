package at.techbee.spectacled.screens.core.data.ai

/**
 * The Claude models offered in the settings selector. [id] is the exact Anthropic model string sent
 * on the API request; [displayName] is the human-facing label (a proper noun, not translated).
 *
 * Ordered cheapest first by Anthropic's list price per token, which is not the same as newest last:
 * Sonnet 5 is both newer and cheaper than Sonnet 4.6, so it sorts before it. [DEFAULT] is the
 * cheapest model that still handles messy input well; it only applies while the user has not picked
 * one themselves, so changing it never overrides an explicit choice.
 */
enum class ClaudeModel(val id: String, val displayName: String) {
    HAIKU_4_5("claude-haiku-4-5", "Claude Haiku 4.5"),
    SONNET_5("claude-sonnet-5", "Claude Sonnet 5"),
    SONNET_4_6("claude-sonnet-4-6", "Claude Sonnet 4.6"),
    OPUS_4_6("claude-opus-4-6", "Claude Opus 4.6"),
    OPUS_5("claude-opus-5", "Claude Opus 5");

    companion object {
        val DEFAULT = SONNET_5

        /** Resolves a stored model id back to an entry, falling back to [DEFAULT] for display. */
        fun fromId(id: String?): ClaudeModel = entries.find { it.id == id } ?: DEFAULT
    }
}
