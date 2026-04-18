package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.PlatformShareManager
import at.techbee.spectacled.screens.core.PlatformSyncTrigger
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    factory { PlatformShareManager(androidContext()) }
    single { DatabaseDriverFactory(androidContext(), get()) }
    single { PlatformCredentialStore(androidContext()) }
    single { PlatformSyncTrigger(androidContext()) }
}