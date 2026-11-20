package at.techbee.spectacled.screens.core.data

/**
 * Decides, from the origin the web build is served from, which CORS-proxy choices it offers and
 * when its proxy setting counts as set up.
 *
 * - **Official origin** (our GitHub Pages deployment): the only origin our hosted proxy accepts
 *   (`PROXY_ALLOWED_ORIGINS` in `fly.toml`), and never the same origin as anyone's CalDAV server -
 *   so a proxy is required, and an empty setting means "not set up yet".
 * - **Any other origin** (a release ZIP unpacked onto someone's own webspace): our hosted proxy
 *   rejects it, but the app may sit on the same origin as the CalDAV server, or talk to one that
 *   sends CORS headers - so an empty setting means "connect directly, without a proxy".
 *
 * Both cases share the one stored value, [UserAppPreferencesStore.userProxyServer]; the two meanings
 * of "empty" never meet because the origin decides which one applies.
 */
object WebProxyPolicy {

    /**
     * Origins that serve the official web build. Keep in sync with `PROXY_ALLOWED_ORIGINS` in
     * `fly.toml`: offering the hosted proxy on an origin it rejects only produces CORS failures.
     */
    val OFFICIAL_ORIGINS: Set<String> = setOf("https://techbeeat.github.io")

    fun isOfficialOrigin(origin: String?): Boolean =
        origin != null && origin.trim().trimEnd('/').lowercase() in OFFICIAL_ORIGINS

    /** Our hosted proxy only accepts requests from the official origin. */
    fun isHostedProxyAvailable(origin: String?): Boolean = isOfficialOrigin(origin)

    /** On the official origin a direct connection can never work, so it is not offered there. */
    fun isDirectConnectionAvailable(origin: String?): Boolean = !isOfficialOrigin(origin)

    /** Whether requests go straight to the CalDAV server because no proxy is set. */
    fun isDirectConnection(origin: String?, userProxyServer: String?): Boolean =
        isDirectConnectionAvailable(origin) && userProxyServer.isNullOrBlank()

    /**
     * Whether the current setting is a usable choice on [origin]. A hosted-proxy choice on an origin
     * that proxy does not accept counts as not set up rather than failing on every request.
     */
    fun isConfigured(origin: String?, userProxyServer: String?): Boolean {
        val proxy = userProxyServer?.trim()
        return when {
            proxy.isNullOrEmpty() -> isDirectConnectionAvailable(origin)
            proxy == HttpClientFactory.HOSTED_FLYIO_PROXY_URL -> isHostedProxyAvailable(origin)
            else -> true
        }
    }
}
