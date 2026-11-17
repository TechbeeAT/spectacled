package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.SpectacledVariant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The split fraction goes straight into `Modifier.weight(f)` / `weight(1f - f)`, which throws for
 * values <= 0 or NaN. Whatever ends up in storage must therefore load as a value in range.
 */
class SplitFractionPreferenceTest {

    private class InMemoryStore : UserAppPreferencesStore {
        val values = mutableMapOf<String, String>()
        override val variant = SpectacledVariant.NOTES
        override fun save(key: String, value: String) { values[key] = value }
        override fun saveEncrypted(key: String, value: String) { values[key] = value }
        override fun load(key: String): String? = values[key]
        override fun loadAsFlow(key: String): Flow<String?> = flowOf(values[key])
        override fun remove(key: String) { values.remove(key) }
    }

    private fun loaded(stored: String?): Float {
        val store = InMemoryStore()
        stored?.let { store.values[SPLIT_FRACTION_LIST_DETAILS] = it }
        return store.splitFractionListDetails
    }

    @Test
    fun missingOrInvalid_fallsBackToDefault() {
        assertEquals(SPLIT_FRACTION_DEFAULT, loaded(null))
        assertEquals(SPLIT_FRACTION_DEFAULT, loaded("abc"))
        assertEquals(SPLIT_FRACTION_DEFAULT, loaded("NaN"))
        assertEquals(SPLIT_FRACTION_DEFAULT, loaded("Infinity"))
    }

    @Test
    fun outOfRange_isClamped() {
        assertEquals(SPLIT_FRACTION_MAX, loaded("1.0"))
        assertEquals(SPLIT_FRACTION_MIN, loaded("0"))
        assertEquals(SPLIT_FRACTION_MIN, loaded("-3"))
    }

    @Test
    fun validValue_roundTrips() {
        val store = InMemoryStore()
        store.splitFractionAccountsList = 0.3f
        assertEquals(0.3f, store.splitFractionAccountsList)
    }

    @Test
    fun save_clampsAndIgnoresNaN() {
        val store = InMemoryStore()
        store.splitFractionAccountsList = 2f
        assertEquals(SPLIT_FRACTION_MAX, store.splitFractionAccountsList)
        store.splitFractionAccountsList = Float.NaN
        assertEquals(SPLIT_FRACTION_MAX, store.splitFractionAccountsList)
    }
}
