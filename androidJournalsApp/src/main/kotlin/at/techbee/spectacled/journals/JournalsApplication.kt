package at.techbee.spectacled.journals

import android.app.Application
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.koin.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class JournalsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@JournalsApplication)
            modules(
                module { single { SpectacledVariant.JOURNALS } },
                sharedModule
            )
        }
    }
}
