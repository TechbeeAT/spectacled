package at.techbee.spectacled.tasks

import android.app.Application
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.initLogging
import at.techbee.spectacled.screens.core.koin.sharedModule
import at.techbee.spectacled.theme.AppLanguage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class TasksApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initLogging()

        startKoin {
            androidLogger()
            androidContext(this@TasksApplication)
            modules(
                module { single { SpectacledVariant.TASKS } },
                sharedModule
            )
        }

        AppLanguage.init(this)
    }
}
