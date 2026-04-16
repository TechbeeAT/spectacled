package at.techbee.spectacled.screens.core

/**
 * A data class to hold the content that will be shared.
 * This makes the API clean and extensible.
 */
data class ShareContent(val subject: String, val body: String)


interface ShareManager {
    fun share(content: ShareContent)
}

expect class PlatformShareManager: ShareManager {
    override fun share(content: ShareContent)
}