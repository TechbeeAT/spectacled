package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.SpectacledVariant
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import eu.anifantakis.lib.ksafe.awaitCacheReady
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In a private session every preference is kept in memory only - except the few in
 * [DEVICE_CONFIGURATION_PREFERENCE_KEYS], which describe this browser's setup rather than its user.
 * The CORS proxy is the reason: without it the web build reaches no CalDAV server at all, so
 * forgetting it would mean re-entering it before every private session.
 */
actual class PlatformUserAppPreferencesStore(
    actual override val variant: SpectacledVariant
): UserAppPreferencesStore {

    private val sessionOnly = getLocalDataPolicy().current == LocalDataPersistence.SESSION_ONLY

    // Constructed in both modes: the device configuration keys keep going to the browser's storage
    // even while a private session is running.
    private val ksafe = KSafe(APP_PREFERENCES_FILE_NAME)

    /** Backing store for everything a private session keeps out of the browser. */
    private val sessionValues = MutableStateFlow<Map<String, String>>(emptyMap())

    private fun isStored(key: String) = !sessionOnly || key in DEVICE_CONFIGURATION_PREFERENCE_KEYS

    actual override fun save(key: String, value: String) {
        if (isStored(key)) ksafe.putDirect(key, value, KSafeWriteMode.Plain)
        else sessionValues.update { it + (key to value) }
    }

    // Nothing to encrypt at rest for a value that never leaves the page.
    actual override fun saveEncrypted(key: String, value: String) {
        if (isStored(key)) ksafe.putDirect(key, value, KSafeWriteMode.Encrypted())
        else sessionValues.update { it + (key to value) }
    }

    actual override fun load(key: String): String? =
        if (isStored(key)) ksafe.getDirect(key, null) else sessionValues.value[key]

    actual override fun loadAsFlow(key: String): Flow<String?> =
        if (isStored(key)) ksafe.getFlow(key, null)
        else sessionValues.map { it[key] }.distinctUntilChanged()

    actual override fun remove(key: String) {
        if (isStored(key)) ksafe.deleteDirect(key)
        else sessionValues.update { it - key }
    }

    override suspend fun awaitReady() = ksafe.awaitCacheReady()
}
