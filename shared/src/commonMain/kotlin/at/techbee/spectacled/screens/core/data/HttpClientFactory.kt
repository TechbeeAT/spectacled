package at.techbee.spectacled.screens.core.data


import at.techbee.spectacled.screens.core.Platforms
import at.techbee.spectacled.screens.core.getPlatform
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.DigestAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.auth.providers.digest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object HttpClientFactory {

    fun create(
        engine: HttpClientEngine,
        username: String? = null,
        password: String? = null,
        jsonContentNegotiation: Boolean = false,
        proxyUrl: String? = if(getPlatform().platform == Platforms.WASM) "http://localhost:8088" else null
    ): HttpClient {
        return HttpClient(engine) {
            if (proxyUrl != null) {
                install("ProxyInterceptor") {
                    requestPipeline.intercept(HttpRequestPipeline.Transform) {
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
                level = LogLevel.ALL
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }

            if (username != null && password != null) {
                install(Auth) {
                    basic {
                        credentials {
                            BasicAuthCredentials(
                                username = username,
                                password = password
                            )
                        }
                        sendWithoutRequest { true }
                    }
                    digest {
                        credentials {
                            DigestAuthCredentials(
                                username = username,
                                password = password
                            )
                        }
                    }
                }
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
