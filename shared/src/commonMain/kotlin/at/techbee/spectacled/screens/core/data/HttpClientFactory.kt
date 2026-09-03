package at.techbee.spectacled.screens.core.data


import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.getPlatform
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object HttpClientFactory {

    /** Proxy the web (WASM) build falls back to when the user hasn't configured one. */
    const val DEFAULT_WEB_PROXY_URL = "http://localhost:8088"

    /**
     * Instance of [the CORS proxy](https://github.com/TechbeeAT/spectacled/tree/main/server) that Techbee
     * hosts for users who can't run their own. It terminates TLS and therefore sees the CalDAV credentials
     * of everyone using it, so it must only ever be set after the user confirmed the trust dialog
     * (see `ProxyTrustDialog` / `UserAppPreferencesStore.hostedProxyConsentUrl`).
     */
    const val HOSTED_WEB_PROXY_URL = "https://spectacled-proxy.fly.dev"

    /** Where the self-hosting instructions for the proxy live. */
    const val PROXY_SETUP_INFO_URL = "https://github.com/TechbeeAT/spectacled/tree/main/server"

    /** The proxy URL to use on the current platform when no user setting is present. */
    fun defaultProxyUrl(): String? =
        if (getPlatform().platform == Platforms.WASM) DEFAULT_WEB_PROXY_URL else null

    fun create(
        engine: HttpClientEngine,
        jsonContentNegotiation: Boolean = true,
        // Resolved per request so the in-app "Proxy server" setting takes effect without an app restart.
        proxyUrlProvider: () -> String? = { defaultProxyUrl() }
    ): HttpClient {
        return HttpClient(engine) {
            followRedirects = false

            install("ProxyInterceptor") {
                requestPipeline.intercept(HttpRequestPipeline.Transform) {
                    val proxyUrl = proxyUrlProvider()?.takeIf { it.isNotBlank() }
                    if (proxyUrl != null) {
                        val originalUrl = context.url.buildString()
                        // Only proxy external requests, not the proxy itself
                        if (originalUrl.startsWith("http") && !originalUrl.startsWith(proxyUrl)) {
                            context.headers.append("X-Target-Url", originalUrl)
                            val pUrl = io.ktor.http.Url(proxyUrl)
                            context.url.protocol = pUrl.protocol
                            context.url.host = pUrl.host
                            context.url.port = pUrl.port
                        }
                    }
                }
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(tag = "HttpClient", message = message)
                    }
                }
                level = LogLevel.HEADERS  // replace with LogLevel.ALL for detailed debug logs
                sanitizeHeader { header ->
                    // sanitizing x-api-key is specifically for Anthropic
                    header == HttpHeaders.Authorization || header.equals("x-api-key", ignoreCase = true)
                }
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }

            if (jsonContentNegotiation) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                            isLenient = true
                        }
                    )
                }
            }
        }
    }
}

expect fun getPlatformEngine(): HttpClientEngine
