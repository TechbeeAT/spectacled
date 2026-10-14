package at.techbee.spectacled.screens.core.data.webdav

private val davErrorMessageRegex = Regex(
    "<(?:[\\w.-]+:)?message(?:\\s[^>]*)?>(.*?)</(?:[\\w.-]+:)?message>",
    RegexOption.DOT_MATCHES_ALL
)

/**
 * Extracts the human-readable message from a DAV error body, e.g. SabreDAV's (Nextcloud, Infomaniak, …)
 * `<d:error><s:message>The VJOURNAL properties is not supported</s:message></d:error>`.
 * Returns null if the body doesn't contain such a message.
 */
fun extractDavErrorMessage(body: String?): String? =
    body?.let { davErrorMessageRegex.find(it) }
        ?.groupValues?.get(1)
        ?.replace("&lt;", "<")?.replace("&gt;", ">")?.replace("&quot;", "\"")?.replace("&apos;", "'")?.replace("&amp;", "&")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

/** Appends the message the server returned (if any) to a user-facing error message. */
fun String.withDavErrorMessage(body: String?): String =
    extractDavErrorMessage(body)?.let { "$this\n$it" } ?: this
