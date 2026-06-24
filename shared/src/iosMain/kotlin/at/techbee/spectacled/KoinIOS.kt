package at.techbee.spectacled

import at.techbee.spectacled.screens.core.koin.sharedModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun initKoinIos(spectacledVariant: SpectacledVariant) {
    if (KoinPlatform.getKoinOrNull() == null) {
        Napier.base(DebugAntilog())
        startKoin {
            modules(
                module { single { spectacledVariant } },
                sharedModule
            )
        }
    }
}
