package at.techbee.spectacled

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform