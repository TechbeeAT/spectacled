package at.techbee.spectacled.screens.core.data.ai

import at.techbee.spectacled.SpectacledVariant

/**
 * A backend that turns free [rawText] into a list of [AiDerivedEntryDto] (parents + nested
 * subtasks). Mapping the DTOs to [at.techbee.spectacled.screens.core.domain.IcalEntry] and
 * persisting them is the caller's job - see [AiDerivedEntryDto.toIcalEntries].
 *
 * Implementations differ only in the transport (which HTTP API they call); the prompt and the
 * response parsing are shared via [buildDeriveEntriesPrompt] and [parseDerivedEntries] so every
 * provider behaves identically.
 */
interface AiDeriveEntriesDataSource {

    /**
     * [variant] tells the model what the top-level entries are for the current list, so it never
     * has to choose a type. [createSubtasks] mirrors the user's toggle. [existingCategories] are
     * the categories already in use so the model prefers reusing them over near-duplicates.
     */
    suspend fun deriveEntries(
        rawText: String,
        variant: SpectacledVariant,
        createSubtasks: Boolean,
        existingCategories: List<String> = emptyList(),
    ): AiDeriveEntriesResult
}
