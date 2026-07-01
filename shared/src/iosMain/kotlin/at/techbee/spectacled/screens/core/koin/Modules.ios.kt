package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.PlatformFileManager
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.data.PlatformUserAppPreferencesStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


actual val platformModule = module {
    singleOf(::DatabaseDriverFactory)
    singleOf(::PlatformCredentialStore)
    singleOf(::PlatformUserAppPreferencesStore)
    singleOf(::PlatformSyncTrigger)
    singleOf(::PlatformShareManager)
    singleOf(::PlatformFileManager)
}
