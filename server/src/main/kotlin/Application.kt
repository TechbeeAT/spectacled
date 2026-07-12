import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
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
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo

const val SERVER_PORT = 8088

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val client = HttpClient(CIO) {
        followRedirects = false
    }

    install(CORS) {
        anyHost() // Allow any host for the proxy
        
        allowHeader("X-Target-Url")
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
        get("/") {
            call.respondText("Spectacled Proxy Server is running. Usage: Set 'X-Target-Url' header to the destination URL.")
        }

        // Catch-all route for proxying
        route("/{proxy...}") {
            handle {
                val targetUrlString = call.request.headers["X-Target-Url"]
                    ?: call.request.queryParameters["target"]
                    ?: return@handle call.respond(HttpStatusCode.BadRequest, "Missing 'X-Target-Url' header or 'target' query parameter")

                val targetUrl = try {
                    Url(targetUrlString)
                } catch (e: Exception) {
                    return@handle call.respond(HttpStatusCode.BadRequest, "Invalid target URL: $targetUrlString")
                }

                println("Proxying ${call.request.httpMethod.value} to: $targetUrl")

                try {
                    val response = client.request(targetUrl) {
                        method = call.request.httpMethod

                        // Copy request headers, excluding hop-by-hop ones
                        call.request.headers.forEach { name, values ->
                            if (!isHopByHopHeader(name) && name != "X-Target-Url") {
                                headers.appendAll(name, values)
                            }
                        }

                        // Forward body for methods that typically include one
                        if (call.request.httpMethod in listOf(
                                HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch,
                                HttpMethod("PROPFIND"), HttpMethod("REPORT"), HttpMethod("PROPPATCH")
                            )
                        ) {
                            setBody(call.receiveChannel())
                        }
                    }

                    call.respond(object : OutgoingContent.WriteChannelContent() {
                        override val status: HttpStatusCode = response.status
                        override val contentType: ContentType? = response.contentType()
                        override val contentLength: Long? = response.contentLength()
                        override val headers: Headers = Headers.build {
                            response.headers.forEach { name, values ->
                                // Skip headers that Ktor will set automatically or that shouldn't be forwarded
                                if (!isHopByHopHeader(name) &&
                                    name != HttpHeaders.ContentType &&
                                    name != HttpHeaders.ContentLength
                                ) {
                                    appendAll(name, values)
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

/**
 * Determines if a header is "hop-by-hop" and should not be forwarded.
 */
private fun isHopByHopHeader(name: String): Boolean =
    name.equals(HttpHeaders.Host, ignoreCase = true) ||
            name.equals(HttpHeaders.TransferEncoding, ignoreCase = true) ||
            name.equals(HttpHeaders.Connection, ignoreCase = true) ||
            //name.equals(HttpHeaders.KeepAlive, ignoreCase = true) ||
            name.equals(HttpHeaders.ProxyAuthenticate, ignoreCase = true) ||
            name.equals(HttpHeaders.ProxyAuthorization, ignoreCase = true) ||
            name.equals(HttpHeaders.TE, ignoreCase = true) ||
            //name.equals(HttpHeaders.Trailers, ignoreCase = true) ||
            name.equals(HttpHeaders.Upgrade, ignoreCase = true)