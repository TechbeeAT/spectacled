package at.techbee.spectacled.screens.core.data

import android.content.Context
import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode


actual class PlatformUserAppPreferencesStore(context: Context): UserAppPreferencesStore {

    private val ksafe = KSafe(context.applicationContext, APP_PREFERENCES_FILE_NAME)

    actual override fun save(key: String, value: String) = ksafe.putDirect(key, value, KSafeWriteMode.Plain)
    actual override fun load(key: String): String? = ksafe.getDirect(key, null)
    actual override fun remove(key: String) = ksafe.deleteDirect(key)
}
