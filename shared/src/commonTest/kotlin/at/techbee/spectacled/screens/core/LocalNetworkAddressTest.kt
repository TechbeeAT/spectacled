package at.techbee.spectacled.screens.core

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNetworkAddressTest {

    @Test
    fun privateIpv4Ranges() {
        listOf(
            "10.0.0.1", "10.255.255.254",
            "172.16.0.1", "172.31.255.254",
            "192.168.1.21",             // the Radicale host this was reported against
            "169.254.10.5",
            "127.0.0.1",
            "10.0.2.2"                  // the Android emulator's host alias
        ).forEach { assertTrue(isPrivateNetworkHost(it), "$it should be private") }
    }

    @Test
    fun publicIpv4AddressesJustOutsideThePrivateRanges() {
        listOf(
            "11.0.0.1",                 // just past 10/8
            "9.255.255.255",
            "172.15.0.1", "172.32.0.1", // either side of 172.16/12
            "192.169.1.1", "192.167.1.1",
            "169.253.0.1", "169.255.0.1",
            "126.0.0.1", "128.0.0.1",
            "8.8.8.8"
        ).forEach { assertFalse(isPrivateNetworkHost(it), "$it should be public") }
    }

    @Test
    fun privateIpv6Addresses() {
        listOf(
            "::1",
            "[::1]",
            "fe80::1", "FE80::1", "feb0::1",
            "fc00::1", "fd12:3456::1",
            "fe80::1%en0",              // zone identifier
            "::ffff:192.168.1.21"       // IPv4-mapped
        ).forEach { assertTrue(isPrivateNetworkHost(it), "$it should be private") }
    }

    @Test
    fun publicIpv6Addresses() {
        listOf(
            "2001:4860:4860::8888",
            "[2606:4700:4700::1111]",
            "fec0::1",                  // site-local, outside fe80::/10 and fc00::/7
            "::ffff:8.8.8.8"
        ).forEach { assertFalse(isPrivateNetworkHost(it), "$it should be public") }
    }

    @Test
    fun mdnsAndSingleLabelHostnames() {
        listOf("raspberrypi.local", "NAS.LOCAL", "nas", "radicale", "server.local.").forEach {
            assertTrue(isPrivateNetworkHost(it), "$it should be private")
        }
    }

    @Test
    fun publicHostnames() {
        listOf(
            "baikal.techbee.at",
            "spectacled.techbee.at",
            "caldav.fastmail.com",
            "example.com"
        ).forEach { assertFalse(isPrivateNetworkHost(it), "$it should be public") }
    }

    @Test
    fun malformedHostsAreNotTreatedAsIpLiterals() {
        // Each has four dot-separated parts but is not a valid dotted quad, so it falls through to
        // the hostname rules - and as a multi-label name, it is treated as public.
        listOf("192.168.1.256", "010.0.0.1", "192.168.1.x", "1.2.3.4.5").forEach {
            assertFalse(isPrivateNetworkHost(it), "$it should not be private")
        }
    }

    @Test
    fun blankHostIsNotPrivate() {
        assertFalse(isPrivateNetworkHost(""))
        assertFalse(isPrivateNetworkHost("   "))
    }
}
