package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformModule = module {
    singleOf(::DatabaseDriverFactory)
    singleOf(::PlatformCredentialStore)
    singleOf(::PlatformSyncTrigger)
    singleOf(::PlatformShareManager)
}