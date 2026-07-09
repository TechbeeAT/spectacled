package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.FileLauncher
import at.techbee.spectacled.screens.core.PlatformFileLauncher
import at.techbee.spectacled.screens.core.PlatformFileManager
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.ShareManager
import at.techbee.spectacled.screens.core.SyncTrigger
import at.techbee.spectacled.screens.core.data.CredentialStore
import at.techbee.spectacled.screens.core.data.PlatformAiSettingsStore
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModule = module {
    singleOf(::DatabaseDriverFactory)
    singleOf(::PlatformCredentialStore) { bind<PlatformCredentialStore>() }
    singleOf(::PlatformAiSettingsStore) { bind<PlatformAiSettingsStore>() }
    singleOf(::PlatformUserAppPreferencesStore) { bind<PlatformUserAppPreferencesStore>() }
    singleOf(::PlatformSyncTrigger) { bind<PlatformSyncTrigger>() }
    singleOf(::PlatformShareManager) { bind<PlatformShareManager>() }
    singleOf(::PlatformFileManager) { bind<PlatformFileManager>() }
    singleOf(::PlatformFileLauncher) { bind<PlatformFileLauncher>() }
}
