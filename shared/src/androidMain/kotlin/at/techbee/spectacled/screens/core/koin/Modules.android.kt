package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.FileLauncher
import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.PlatformFileLauncher
import at.techbee.spectacled.screens.core.PlatformFileManager
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.ShareManager
import at.techbee.spectacled.screens.core.SyncTrigger
import at.techbee.spectacled.screens.core.data.CredentialStore
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformModule = module {
    single { DatabaseDriverFactory(androidContext(), get()) }
    single<PlatformCredentialStore> { PlatformCredentialStore(androidContext()) }.bind<CredentialStore>()
    single<PlatformUserAppPreferencesStore> { PlatformUserAppPreferencesStore(androidContext(), get()) }.bind<UserAppPreferencesStore>()
    single<PlatformSyncTrigger> { PlatformSyncTrigger(androidContext()) }.bind<SyncTrigger>()
    single<PlatformShareManager> { PlatformShareManager(androidContext()) }.bind<ShareManager>()
    single<PlatformFileManager> { PlatformFileManager(androidContext()) }.bind<FileManager>()
    single<PlatformFileLauncher> { PlatformFileLauncher(androidContext()) }.bind<FileLauncher>()
}
