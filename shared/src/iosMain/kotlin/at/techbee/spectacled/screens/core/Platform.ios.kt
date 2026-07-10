package at.techbee.spectacled.screens.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val platform: Platforms = Platforms.IOS
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()


actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO