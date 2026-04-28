package at.techbee.spectacled

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.options
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo

const val SERVER_PORT = 8080
fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "127.0.0.1", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val client = HttpClient(CIO) {
        followRedirects = false
    }

    install(CORS) {
        // Echo back the Origin header to support allowCredentials = true
        // We allow common development ports for Compose HTML/Wasm
        val allowedHosts = listOf("localhost", "127.0.0.1")
        val allowedPorts = listOf(8080, 8081, 8082, 8083, 8084, 8085)

        allowedHosts.forEach { host ->
            allowHost(host) // No port (default 80)
            allowedPorts.forEach { port ->
                allowHost("$host:$port")
            }
        }

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("Depth")
        allowHeader("Destination")
        allowHeader("If-Match")
        allowHeader("If-None-Match")
        allowHeader("Overwrite")
        allowHeader("If")

        exposeHeader(HttpHeaders.ETag)
        exposeHeader(HttpHeaders.Location)
        exposeHeader("DAV")
        exposeHeader("Schedule-Tag")

        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        // WebDAV methods
        allowMethod(HttpMethod("PROPFIND"))
        allowMethod(HttpMethod("PROPPATCH"))
        allowMethod(HttpMethod("MKCOL"))
        allowMethod(HttpMethod("COPY"))
        allowMethod(HttpMethod("MOVE"))
        allowMethod(HttpMethod("LOCK"))
        allowMethod(HttpMethod("UNLOCK"))
        allowMethod(HttpMethod("REPORT"))

        allowCredentials = true
        allowNonSimpleContentTypes = true
    }

    routing {
        // 1. Explicitly catch ANY OPTIONS request so it never gets proxied
        options("/{proxy...}") {
            call.respond(HttpStatusCode.OK)
        }

        get("/") {
            call.respondText("Spectacled Proxy Server is running. Proxying to https://nextcloud.techbee.at/remote.php/dav")
        }

        // Proxy all other requests to Nextcloud
        route("/{proxy...}") {
            handle {
                // Double check to ensure OPTIONS never leaks through
                if (call.request.httpMethod == HttpMethod.Options) {
                    call.respond(HttpStatusCode.OK)
                    return@handle
                }

                val path = call.parameters.getAll("proxy")?.joinToString("/") ?: ""
                val targetBaseUrl = "https://nextcloud.techbee.at/remote.php/dav"
                val proxyUrl = if (path.isEmpty()) targetBaseUrl else "$targetBaseUrl/$path"

                println("Proxying ${call.request.httpMethod.value} to: $proxyUrl")

                try {
                    val response = client.request(proxyUrl) {
                        method = call.request.httpMethod

                        headers {
                            call.request.headers.forEach { name, values ->
                                if (!name.equals(HttpHeaders.Host, ignoreCase = true) &&
                                    !name.equals(HttpHeaders.ContentLength, ignoreCase = true) &&
                                    !name.equals(HttpHeaders.TransferEncoding, ignoreCase = true)
                                ) {
                                    values.forEach { append(name, it) }
                                }
                            }
                        }

                        if (call.request.httpMethod in listOf(
                                HttpMethod.Post,
                                HttpMethod.Put,
                                HttpMethod.Patch,
                                HttpMethod("PROPFIND"),
                                HttpMethod("REPORT"),
                                HttpMethod("PROPPATCH")
                            )
                        ) {
                            setBody(call.receiveChannel())
                        }
                    }

                    call.respond(object : OutgoingContent.WriteChannelContent() {
                        override val contentLength: Long? = response.contentLength()
                        override val contentType: ContentType? = response.contentType()
                        override val status: HttpStatusCode = response.status
                        override val headers: Headers = Headers.build {
                            response.headers.forEach { name, values ->
                                if (!name.equals(HttpHeaders.TransferEncoding, ignoreCase = true) &&
                                    !name.equals(HttpHeaders.ContentLength, ignoreCase = true) &&
                                    !name.equals(HttpHeaders.ContentType, ignoreCase = true)
                                ) {
                                    values.forEach { append(name, it) }
                                }
                            }
                        }

                        override suspend fun writeTo(channel: ByteWriteChannel) {
                            response.bodyAsChannel().copyTo(channel)
                        }
                    })
                } catch (e: Exception) {
                    println("Proxy error: ${e.message}")
                    call.respond(HttpStatusCode.InternalServerError, "Proxy error: ${e.message}")
                }
            }
        }
    }
}