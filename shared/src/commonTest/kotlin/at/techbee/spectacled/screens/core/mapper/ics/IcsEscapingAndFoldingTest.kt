package at.techbee.spectacled.screens.core.mapper.ics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IcsEscapingAndFoldingTest {

    /** Serializes [original] as a property value and parses it back, i.e. escape -> unescape. */
    private fun roundTrip(original: String): String =
        parseProperty("X-TEST:${escapeIcsValue(original)}").value

    @Test
    fun escapeUnescape_plainText() {
        assertEquals("plain text", roundTrip("plain text"))
    }

    @Test
    fun escapeUnescape_reservedCharacters() {
        assertEquals("with, comma; and\nnewline", roundTrip("with, comma; and\nnewline"))
    }

    @Test
    fun escapeUnescape_singleBackslash() {
        assertEquals("back \\ slash", roundTrip("back \\ slash"))
    }

    @Test
    fun escapeUnescape_backslashFollowedByN() {
        // Regression test: "\n" here is a literal backslash followed by the letter n
        // (e.g. a Windows path in a note). A sequential-replace unescape misreads the
        // escaped form and corrupts it into a real newline.
        assertEquals("C:\\Users\\name", roundTrip("C:\\Users\\name"))
        assertEquals("literal \\n sequence", roundTrip("literal \\n sequence"))
    }

    @Test
    fun escapeUnescape_backslashFollowedByComma() {
        assertEquals("odd \\, but legal", roundTrip("odd \\, but legal"))
    }

    @Test
    fun escapeUnescape_doubledAndTrailingBackslash() {
        assertEquals("double \\\\ backslash", roundTrip("double \\\\ backslash"))
        assertEquals("trailing backslash \\", roundTrip("trailing backslash \\"))
    }

    @Test
    fun unescape_acceptsUppercaseNewlineFromOtherProducers() {
        // RFC 5545 allows \N as an alternative newline escape; this app never produces
        // it, but other CalDAV clients may.
        assertEquals("a\nb", unescapeIcsValue("a\\Nb"))
    }

    @Test
    fun unescape_leavesUnknownEscapesAndLoneTrailingBackslashIntact() {
        assertEquals("\\x", unescapeIcsValue("\\x"))
        assertEquals("abc\\", unescapeIcsValue("abc\\"))
    }

    @Test
    fun splitIcsList_separatesOnUnescapedCommasOnly() {
        assertEquals(listOf("Personal", "Ideas"), splitIcsList("Personal,Ideas"))
        assertEquals(listOf("Work, Private"), splitIcsList("Work\\, Private"))
        assertEquals(listOf("a,b", "c"), splitIcsList("a\\,b,c"))
    }

    @Test
    fun splitIcsList_handlesBackslashHeavyElements() {
        // A category ending in a backslash, escaped to "\\", directly followed by the
        // separator comma - the scanner must not treat that comma as escaped.
        assertEquals(listOf("ends with backslash\\", "next"), splitIcsList("ends with backslash\\\\,next"))
        assertEquals(listOf("semi;colon", "back\\slash"), splitIcsList("semi\\;colon,back\\\\slash"))
    }

    @Test
    fun splitIcsList_dropsEmptyElements() {
        assertEquals(listOf("A", "B"), splitIcsList("A,,B"))
        assertEquals(emptyList<String>(), splitIcsList(""))
    }

    @Test
    fun splitIcsList_roundTripsThroughPerElementEscaping() {
        val categories = listOf("Work, Private", "a,b", "semi;colon", "back\\slash", "line\nbreak")
        val raw = categories.joinToString(",") { escapeIcsValue(it) }
        assertEquals(categories, splitIcsList(raw))
    }

    @Test
    fun foldIcsLine_lineAtLimitIsUnchanged() {
        val line = "A".repeat(75)
        assertEquals(line, foldIcsLine(line))
    }

    @Test
    fun foldIcsLine_longLineIsFoldedWithSpaceContinuation() {
        val folded = foldIcsLine("A".repeat(80))
        assertEquals("A".repeat(75) + "\r\n " + "A".repeat(5), folded)
    }

    @Test
    fun foldIcsLine_everyContinuationLineStartsWithSpace() {
        val folded = foldIcsLine("B".repeat(200))
        folded.split("\r\n").drop(1).forEach { continuation ->
            assertTrue(continuation.startsWith(" "), "Continuation line must start with a space: '$continuation'")
        }
    }

    @Test
    fun unfoldLines_isInverseOfFoldIcsLine() {
        val line = "DESCRIPTION:" + "Lorem ipsum dolor sit amet, consetetur sadipscing elitr. ".repeat(5)
        assertEquals(listOf(line), unfoldLines(foldIcsLine(line)))
    }

    @Test
    fun unfoldLines_supportsSpaceAndTabContinuations() {
        assertEquals(listOf("AB"), unfoldLines("A\r\n B"))
        assertEquals(listOf("AB"), unfoldLines("A\r\n\tB"))
    }

    @Test
    fun unfoldLines_keepsSeparateLinesSeparate() {
        assertEquals(listOf("LINE1:a", "LINE2:b"), unfoldLines("LINE1:a\r\nLINE2:b"))
    }

    @Test
    fun unfoldLines_handlesBareNewlinesAsWell() {
        // Some servers send LF-only line endings.
        assertEquals(listOf("SUMMARY:abcdef"), unfoldLines("SUMMARY:abc\n def"))
    }
}
