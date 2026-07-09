package at.techbee.spectacled.screens.core.data

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


object HttpClientFactory {

    fun create(
        engine: HttpClientEngine,
        jsonContentNegotiation: Boolean = true
    ): HttpClient {
        return HttpClient(engine) {
            followRedirects = false
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(tag = "HttpClient", message = message)
                    }
                }
                level = LogLevel.ALL
                sanitizeHeader { header ->
                    // sanitizing x-api-key is specifically for Anthropic and might be removed in the future
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
