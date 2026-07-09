package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformFileLauncher
import at.techbee.spectacled.screens.core.PlatformFileManager
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformAiSettingsStore
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory(androidContext(), get()) }
    single<PlatformCredentialStore> { PlatformCredentialStore(androidContext()) }
    single<PlatformAiSettingsStore> { PlatformAiSettingsStore(androidContext()) }
    single<PlatformUserAppPreferencesStore> { PlatformUserAppPreferencesStore(androidContext()) }
    single<PlatformSyncTrigger> { PlatformSyncTrigger(androidContext()) }
    single<PlatformShareManager> { PlatformShareManager(androidContext()) }
    single<PlatformFileManager> { PlatformFileManager(androidContext()) }
    single<PlatformFileLauncher> { PlatformFileLauncher(androidContext()) }
}
