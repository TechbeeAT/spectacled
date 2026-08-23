package at.techbee.spectacled

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
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.content.OutgoingContent
import io.ktor.http.takeFrom
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.Inet6Address
import java.net.InetAddress

private val logger = LoggerFactory.getLogger("SpectacledProxy")

const val DEFAULT_SERVER_PORT = 8088

/**
 * Runtime configuration for the proxy, read from environment variables so the same
 * image can be self-hosted or run as a demo instance without code changes.
 *
 * @param port                 TCP port to bind. `PORT` (PaaS hosts inject this).
 * @param allowedOrigins       CORS allow-list of web origins (e.g. `https://spectacled.techbee.at`).
 *                             Empty = reflect any origin (development only — logged as a warning).
 * @param allowedTargetHosts   Allow-list of destination hostnames. Empty = any host permitted
 *                             (private addresses are still blocked unless [allowPrivateTargets]).
 * @param requireHttpsTarget   Reject non-https target URLs. Defaults to true.
 * @param allowPrivateTargets  Permit targets that resolve to loopback/link-local/private ranges.
 *                             Defaults to false; the SSRF guard that blocks them is the main reason
 *                             this proxy is safe to expose publicly.
 */
data class ProxyConfig(
    val port: Int,
    val allowedOrigins: List<String>,
    val allowedTargetHosts: List<String>,
    val requireHttpsTarget: Boolean,
    val allowPrivateTargets: Boolean,
) {
    companion object {
        fun fromEnv(): ProxyConfig {
            fun csv(name: String): List<String> =
                System.getenv(name)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

            fun bool(name: String, default: Boolean): Boolean =
                System.getenv(name)?.trim()?.toBooleanStrictOrNull() ?: default

            return ProxyConfig(
                port = System.getenv("PORT")?.trim()?.toIntOrNull() ?: DEFAULT_SERVER_PORT,
                allowedOrigins = csv("PROXY_ALLOWED_ORIGINS"),
                allowedTargetHosts = csv("PROXY_ALLOWED_TARGET_HOSTS"),
                requireHttpsTarget = bool("PROXY_REQUIRE_HTTPS_TARGET", default = true),
                allowPrivateTargets = bool("PROXY_ALLOW_PRIVATE_TARGETS", default = false),
            )
        }
    }
}

fun main() {
    val config = ProxyConfig.fromEnv()
    logger.info(
        "Starting Spectacled proxy on port {} (allowedOrigins={}, allowedTargetHosts={}, requireHttps={}, allowPrivate={})",
        config.port,
        config.allowedOrigins.ifEmpty { "<any>" },
        config.allowedTargetHosts.ifEmpty { "<any>" },
        config.requireHttpsTarget,
        config.allowPrivateTargets,
    )
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: ProxyConfig = ProxyConfig.fromEnv()) {
    val client = HttpClient(CIO) {
        followRedirects = false
    }

    install(CORS) {
        if (config.allowedOrigins.isEmpty()) {
            logger.warn(
                "PROXY_ALLOWED_ORIGINS is not set — reflecting ANY origin. " +
                    "This is fine for local development but must be set when hosting publicly."
            )
            anyHost()
        } else {
            config.allowedOrigins.forEach { origin ->
                val scheme = origin.substringBefore("://", missingDelimiterValue = "https")
                val hostWithPort = origin.substringAfter("://")
                allowHost(hostWithPort, schemes = listOf(scheme))
            }
        }

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
            call.respondText(
                "Spectacled Proxy Server is running. Usage: set the 'X-Target-Url' header to the " +
                    "destination CalDAV URL. See https://github.com/TechbeeAT/spectacled/tree/main/server"
            )
        }

        // Catch-all route for proxying
        route("/{proxy...}") {
            handle {
                val targetUrlString = call.request.headers["X-Target-Url"]
                    ?: call.request.queryParameters["target"]
                    ?: return@handle call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing 'X-Target-Url' header or 'target' query parameter"
                    )

                val targetUrl = when (val validation = validateTarget(targetUrlString, config)) {
                    is TargetValidation.Ok -> validation.url
                    is TargetValidation.Rejected -> {
                        logger.warn("Rejected proxy target '{}': {}", targetUrlString, validation.reason)
                        return@handle call.respond(HttpStatusCode.Forbidden, "Target rejected: ${validation.reason}")
                    }
                }

                logger.info("Proxying {} to host {}", call.request.httpMethod.value, targetUrl.host)

                // Buffer the request body once so it can be replayed if we follow redirects.
                // CalDAV request bodies (PROPFIND/REPORT/LOCK XML) are small.
                val requestBody: ByteArray? =
                    if (requestHasBody(call.request.headers)) call.receive<ByteArray>() else null

                try {
                    var currentUrl = targetUrl
                    var redirectsRemaining = MAX_REDIRECTS
                    while (true) {
                        val response = client.request(currentUrl) {
                            method = call.request.httpMethod

                            // Copy request headers, excluding hop-by-hop and browser-added ones.
                            call.request.headers.forEach { name, values ->
                                if (!isHopByHopHeader(name) && !isStrippedRequestHeader(name)) {
                                    headers.appendAll(name, values)
                                }
                            }

                            // Forward the body whenever the incoming request carried one. Keying off the
                            // presence of a body (not a method allow-list) means WebDAV LOCK — which sends
                            // an XML lock-info body — is forwarded correctly.
                            if (requestBody != null) {
                                setBody(requestBody)
                            }
                        }

                        // CalDAV discovery (e.g. /.well-known/caldav) relies on redirects. A browser
                        // can't follow a cross-origin redirect to the target itself, so the proxy resolves
                        // them here — re-validating every hop so a malicious redirect can't slip past the
                        // SSRF / host-allow-list checks. The original request method and body are replayed
                        // (RFC 6764 discovery repeats the request at the new location).
                        val location = response.headers[HttpHeaders.Location]
                        if (response.status.value in REDIRECT_STATUS_CODES && location != null && redirectsRemaining > 0) {
                            redirectsRemaining--
                            val resolved = resolveRedirect(currentUrl, location)
                            when (val validation = validateTarget(resolved, config)) {
                                is TargetValidation.Ok -> {
                                    logger.info("Following redirect to host {}", validation.url.host)
                                    currentUrl = validation.url
                                    continue
                                }
                                is TargetValidation.Rejected -> {
                                    logger.warn("Rejected redirect target '{}': {}", resolved, validation.reason)
                                    return@handle call.respond(
                                        HttpStatusCode.Forbidden,
                                        "Redirect target rejected: ${validation.reason}"
                                    )
                                }
                            }
                        }

                        call.respond(object : OutgoingContent.WriteChannelContent() {
                            override val status: HttpStatusCode = response.status
                            override val contentType: ContentType? = response.contentType()
                            override val contentLength: Long? = response.contentLength()
                            override val headers: Headers = Headers.build {
                                response.headers.forEach { name, values ->
                                    // Skip headers Ktor sets automatically, and any CORS headers the
                                    // upstream sent — the CORS plugin here is the single source of truth.
                                    if (!isHopByHopHeader(name) &&
                                        name != HttpHeaders.ContentType &&
                                        name != HttpHeaders.ContentLength &&
                                        !isStrippedResponseHeader(name)
                                    ) {
                                        appendAll(name, values)
                                    }
                                }
                            }

                            override suspend fun writeTo(channel: ByteWriteChannel) {
                                response.bodyAsChannel().copyTo(channel)
                            }
                        })
                        return@handle
                    }
                } catch (e: Exception) {
                    logger.warn("Proxy error talking to {}: {}", targetUrl.host, e.message)
                    call.respond(HttpStatusCode.BadGateway, "Proxy error: ${e.message}")
                }
            }
        }
    }
}

sealed class TargetValidation {
    data class Ok(val url: Url) : TargetValidation()
    data class Rejected(val reason: String) : TargetValidation()
}

/**
 * Validates a caller-supplied target URL before the proxy forwards to it. This is the core
 * defense that stops the proxy from being an open relay / SSRF pivot: it enforces the scheme,
 * an optional host allow-list, and (unless explicitly disabled) blocks any target that resolves
 * to a loopback/link-local/private/unique-local address such as cloud metadata (169.254.169.254).
 */
suspend fun validateTarget(raw: String, config: ProxyConfig): TargetValidation {
    val url = try {
        Url(raw)
    } catch (e: Exception) {
        return TargetValidation.Rejected("invalid URL")
    }

    if (url.host.isBlank()) return TargetValidation.Rejected("missing host")

    val scheme = url.protocol.name.lowercase()
    if (scheme != "http" && scheme != "https") return TargetValidation.Rejected("unsupported scheme")
    if (config.requireHttpsTarget && scheme != "https") return TargetValidation.Rejected("https target required")

    if (config.allowedTargetHosts.isNotEmpty() &&
        config.allowedTargetHosts.none { it.equals(url.host, ignoreCase = true) }
    ) {
        return TargetValidation.Rejected("target host not on allow-list")
    }

    if (!config.allowPrivateTargets) {
        val addresses = try {
            withContext(Dispatchers.IO) { InetAddress.getAllByName(url.host) }
        } catch (e: Exception) {
            return TargetValidation.Rejected("could not resolve target host")
        }
        if (addresses.any { it.isPrivateOrLocal() }) {
            return TargetValidation.Rejected("target resolves to a private or local address")
        }
    }

    return TargetValidation.Ok(url)
}

/**
 * True for addresses that must never be reachable through a public proxy: loopback, wildcard,
 * link-local (incl. IPv4 169.254/16 and IPv6 fe80::/10), site-local/private IPv4 ranges,
 * multicast, and IPv6 unique-local (fc00::/7).
 */
private fun InetAddress.isPrivateOrLocal(): Boolean {
    if (isLoopbackAddress || isAnyLocalAddress || isLinkLocalAddress || isSiteLocalAddress || isMulticastAddress) {
        return true
    }
    if (this is Inet6Address) {
        val firstByte = address.firstOrNull()?.toInt()?.and(0xfe) ?: return false
        if (firstByte == 0xfc) return true // fc00::/7 unique local
    }
    return false
}

private fun requestHasBody(headers: Headers): Boolean {
    val contentLength = headers[HttpHeaders.ContentLength]?.toLongOrNull()
    if (contentLength != null) return contentLength > 0
    return headers[HttpHeaders.TransferEncoding]?.contains("chunked", ignoreCase = true) == true
}

private const val MAX_REDIRECTS = 5

private val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)

/**
 * Resolves a redirect `Location` (absolute URL, absolute path, or relative reference) against the
 * URL it was returned from, so the result can be re-validated and requested.
 */
private fun resolveRedirect(current: Url, location: String): String = when {
    location.startsWith("http://", ignoreCase = true) ||
        location.startsWith("https://", ignoreCase = true) -> location

    location.startsWith("/") -> {
        val portPart = if (current.port == current.protocol.defaultPort) "" else ":${current.port}"
        "${current.protocol.name}://${current.host}$portPart$location"
    }

    else -> URLBuilder(current).apply { takeFrom(location) }.buildString()
}

/**
 * Determines if a header is "hop-by-hop" and should not be forwarded.
 */
private fun isHopByHopHeader(name: String): Boolean =
    name.equals(HttpHeaders.Host, ignoreCase = true) ||
            name.equals(HttpHeaders.TransferEncoding, ignoreCase = true) ||
            name.equals(HttpHeaders.Connection, ignoreCase = true) ||
            name.equals(HttpHeaders.ProxyAuthenticate, ignoreCase = true) ||
            name.equals(HttpHeaders.ProxyAuthorization, ignoreCase = true) ||
            name.equals(HttpHeaders.TE, ignoreCase = true) ||
            name.equals(HttpHeaders.Upgrade, ignoreCase = true)

/**
 * Request headers we deliberately don't forward to the target: the routing header itself,
 * the incoming Content-Length (the outbound body is re-framed by the client), and browser
 * context headers that would confuse or leak information to the upstream server.
 */
private fun isStrippedRequestHeader(name: String): Boolean =
    name.equals("X-Target-Url", ignoreCase = true) ||
            name.equals(HttpHeaders.ContentLength, ignoreCase = true) ||
            name.equals(HttpHeaders.Origin, ignoreCase = true) ||
            name.equals("Referer", ignoreCase = true) ||
            name.equals(HttpHeaders.Cookie, ignoreCase = true)

/**
 * Response headers we drop so the CORS plugin remains the single source of truth for
 * cross-origin headers (avoids duplicated/conflicting Access-Control-* on the response).
 */
private fun isStrippedResponseHeader(name: String): Boolean =
    name.startsWith("Access-Control-", ignoreCase = true)
