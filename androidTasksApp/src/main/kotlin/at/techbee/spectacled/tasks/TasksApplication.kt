package at.techbee.spectacled.tasks

import android.app.Application
import android.content.pm.ApplicationInfo
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.AppLogging
import at.techbee.spectacled.screens.core.koin.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.module

class TasksApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppLogging.init(isDebugBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)

        startKoin {
            androidLogger()
            androidContext(this@TasksApplication)
            modules(
                module { single { SpectacledVariant.TASKS } },
                sharedModule
            )
        }
    }
}
