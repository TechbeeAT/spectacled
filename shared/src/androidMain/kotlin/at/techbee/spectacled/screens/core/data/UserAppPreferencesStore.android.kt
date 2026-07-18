package at.techbee.spectacled.screens.core.data

import android.content.Context
import at.techbee.spectacled.SpectacledVariant
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow


actual class PlatformUserAppPreferencesStore(
    context: Context,
    actual override val variant: SpectacledVariant
): UserAppPreferencesStore {

    private val ksafe = KSafe(context.applicationContext, APP_PREFERENCES_FILE_NAME)

    actual override fun save(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Plain)
    actual override fun saveEncrypted(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Encrypted())
    actual override fun load(key: String): String? = ksafe.getDirect(key, null)
    actual override fun loadAsFlow(key: String): Flow<String?> = ksafe.getFlow(key, null)
    actual override fun remove(key: String) = ksafe.deleteDirect(key)
}
