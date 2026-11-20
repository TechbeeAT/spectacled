package at.techbee.spectacled.screens.core.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebProxyPolicyTest {

    private val official = "https://techbeeat.github.io"
    private val selfHosted = "https://cloud.example.com"
    private val hosted = HttpClientFactory.HOSTED_FLYIO_PROXY_URL

    @Test
    fun officialOrigin_isMatchedExactly() {
        assertTrue(WebProxyPolicy.isOfficialOrigin(official))
        assertTrue(WebProxyPolicy.isOfficialOrigin("HTTPS://TechbeeAT.github.io/"))
        // A fork's Pages site is GitHub too, but our proxy does not accept it.
        assertFalse(WebProxyPolicy.isOfficialOrigin("https://someone.github.io"))
        assertFalse(WebProxyPolicy.isOfficialOrigin("http://techbeeat.github.io"))
        assertFalse(WebProxyPolicy.isOfficialOrigin(null))
    }

    @Test
    fun officialOrigin_offersHostedProxyButNoDirectConnection() {
        assertTrue(WebProxyPolicy.isHostedProxyAvailable(official))
        assertFalse(WebProxyPolicy.isDirectConnectionAvailable(official))
    }

    @Test
    fun otherOrigin_offersDirectConnectionButNoHostedProxy() {
        assertFalse(WebProxyPolicy.isHostedProxyAvailable(selfHosted))
        assertTrue(WebProxyPolicy.isDirectConnectionAvailable(selfHosted))
    }

    @Test
    fun emptySetting_isNotConfiguredOnOfficialOrigin() {
        assertFalse(WebProxyPolicy.isConfigured(official, null))
        assertFalse(WebProxyPolicy.isConfigured(official, "  "))
        assertFalse(WebProxyPolicy.isDirectConnection(official, null))
    }

    @Test
    fun emptySetting_meansDirectConnectionOnOtherOrigin() {
        assertTrue(WebProxyPolicy.isConfigured(selfHosted, null))
        assertTrue(WebProxyPolicy.isDirectConnection(selfHosted, null))
        assertTrue(WebProxyPolicy.isDirectConnection(selfHosted, ""))
    }

    @Test
    fun ownProxy_isConfiguredEverywhere() {
        assertTrue(WebProxyPolicy.isConfigured(official, "https://proxy.example.com"))
        assertTrue(WebProxyPolicy.isConfigured(selfHosted, "https://proxy.example.com"))
        assertFalse(WebProxyPolicy.isDirectConnection(selfHosted, "https://proxy.example.com"))
    }

    @Test
    fun hostedProxy_isOnlyConfiguredOnOfficialOrigin() {
        assertTrue(WebProxyPolicy.isConfigured(official, hosted))
        assertFalse(WebProxyPolicy.isConfigured(selfHosted, hosted))
    }
}
