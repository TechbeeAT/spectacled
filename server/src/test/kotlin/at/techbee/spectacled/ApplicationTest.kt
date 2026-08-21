package at.techbee.spectacled

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    private fun config(
        allowedOrigins: List<String> = listOf("https://app.example"),
        allowedTargetHosts: List<String> = emptyList(),
        requireHttpsTarget: Boolean = false,
        allowPrivateTargets: Boolean = true,
    ) = ProxyConfig(
        port = 0,
        allowedOrigins = allowedOrigins,
        allowedTargetHosts = allowedTargetHosts,
        requireHttpsTarget = requireHttpsTarget,
        allowPrivateTargets = allowPrivateTargets,
    )

    @Test
    fun rootReturnsBanner() = testApplication {
        application { module(config()) }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Spectacled Proxy Server"))
    }

    @Test
    fun missingTargetIsBadRequest() = testApplication {
        application { module(config()) }
        val response = client.get("/anything")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun targetHostNotOnAllowListIsForbidden() = testApplication {
        application { module(config(allowedTargetHosts = listOf("allowed.example"))) }
        val response = client.get("/dav") {
            header("X-Target-Url", "https://blocked.example/dav")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun nonHttpsTargetIsForbiddenWhenHttpsRequired() = testApplication {
        application { module(config(requireHttpsTarget = true)) }
        val response = client.get("/dav") {
            header("X-Target-Url", "http://allowed.example/dav")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun privateTargetIsForbidden() = testApplication {
        application { module(config(allowPrivateTargets = false)) }
        val response = client.get("/dav") {
            header("X-Target-Url", "http://127.0.0.1/dav")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun corsPreflightEchoesAllowedOrigin() = testApplication {
        application { module(config(allowedOrigins = listOf("https://app.example"))) }
        val response = client.options("/dav") {
            header(HttpHeaders.Origin, "https://app.example")
            header(HttpHeaders.AccessControlRequestMethod, "PROPFIND")
        }
        assertEquals("https://app.example", response.headers[HttpHeaders.AccessControlAllowOrigin])
    }

    @Test
    fun proxiesRequestToAllowedTarget() = testApplication {
        // A real upstream server the proxy forwards to over a loopback socket.
        val upstream = embeddedServer(Netty, port = 0) {
            routing { get("/echo") { call.respondText("hello-from-upstream") } }
        }
        upstream.start(wait = false)
        try {
            val upstreamPort = upstream.engine.resolvedConnectors().first().port
            application { module(config()) }

            val response = client.get("/echo") {
                header("X-Target-Url", "http://127.0.0.1:$upstreamPort/echo")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("hello-from-upstream", response.bodyAsText())
        } finally {
            upstream.stop(gracePeriodMillis = 0, timeoutMillis = 500)
        }
    }
}
