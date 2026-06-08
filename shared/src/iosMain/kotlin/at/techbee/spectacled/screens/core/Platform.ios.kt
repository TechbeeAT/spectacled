package at.techbee.spectacled.screens.core

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val platform: Platforms = Platforms.IOS
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
