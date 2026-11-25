package at.techbee.spectacled.screens.core

/**
 * Whether [host] names a machine on the user's own network rather than somewhere on the internet.
 *
 * Used to decide whether a permission that only governs local network access is worth mentioning
 * at all - someone syncing with a hosted CalDAV provider should never see a "nearby devices"
 * prompt, and someone syncing with a box in their hallway should.
 *
 * Matches on the literal host, so a DNS name that happens to resolve into a private range (a
 * hostname pointed at 192.168.x.y, say) is not recognised. Resolving it would mean a DNS lookup,
 * which is unavailable in commonMain and would have to happen before the UI could render; the
 * cost of the gap is a missed hint, not a broken connection.
 */
fun isPrivateNetworkHost(host: String): Boolean {
    // Ktor hands IPv6 hosts over bracketed in some code paths and bare in others.
    val bare = host.trim().removeSurrounding("[", "]").substringBefore('%').lowercase()
    if (bare.isEmpty()) return false

    parseIpv4(bare)?.let { return it.isPrivateIpv4() }
    if (bare.contains(':')) return bare.isPrivateIpv6()

    // A trailing dot marks a fully qualified name; ".local" is mDNS, and a name with no dot at
    // all is a short LAN hostname ("nas", "raspberrypi") that only a local resolver can answer.
    val name = bare.trimEnd('.')
    return name.endsWith(".local") || name == "local" || !name.contains('.')
}

/** The four octets of [host], or null if it is not a dotted-quad IPv4 literal. */
private fun parseIpv4(host: String): List<Int>? {
    val parts = host.split('.')
    if (parts.size != 4) return null

    return parts.map { part ->
        // Reject "01", "+1" and the like: only a plain decimal octet is an IPv4 literal.
        if (part.isEmpty() || part.length > 3 || !part.all { it.isDigit() }) return null
        if (part.length > 1 && part[0] == '0') return null
        part.toInt().also { if (it > 255) return null }
    }
}

private fun List<Int>.isPrivateIpv4(): Boolean {
    val (a, b) = this
    return when {
        a == 10 -> true                     // 10.0.0.0/8
        a == 172 && b in 16..31 -> true     // 172.16.0.0/12
        a == 192 && b == 168 -> true        // 192.168.0.0/16
        a == 169 && b == 254 -> true        // 169.254.0.0/16 link-local
        a == 127 -> true                    // 127.0.0.0/8 loopback
        else -> false
    }
}

private fun String.isPrivateIpv6(): Boolean {
    val address = this
    if (address == "::1") return true

    // An IPv4-mapped address ("::ffff:192.168.1.21") is really the IPv4 address it carries.
    address.substringAfterLast(':').let { tail ->
        parseIpv4(tail)?.let { return it.isPrivateIpv4() }
    }

    return when {
        address.startsWith("fe8") || address.startsWith("fe9") ||
            address.startsWith("fea") || address.startsWith("feb") -> true   // fe80::/10 link-local
        address.startsWith("fc") || address.startsWith("fd") -> true         // fc00::/7 unique local
        else -> false
    }
}
