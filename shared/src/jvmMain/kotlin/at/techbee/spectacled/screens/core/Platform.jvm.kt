package at.techbee.spectacled.screens.core

class JVMPlatform: Platform {
    override val platform: Platforms = Platforms.DESKTOP
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
