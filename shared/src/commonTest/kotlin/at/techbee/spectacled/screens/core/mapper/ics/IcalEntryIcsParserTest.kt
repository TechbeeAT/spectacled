package at.techbee.spectacled.screens.core.mapper.ics

import kotlin.test.Test
import kotlin.test.assertEquals

class IcalEntryIcsParserTest {

    @Test
    fun parseProperty_noParams() {
        val result = parseProperty("DTSTART:20260710T120000Z")
        assertEquals("DTSTART", result.name)
        assertEquals(emptyMap(), result.params)
        assertEquals("20260710T120000Z", result.value)
    }

    @Test
    fun parseProperty_unquotedParamValueContainingColon() {
        // The value itself contains ':' (a mailto URI) - only the *first* unquoted ':'
        // may separate params from value, everything after belongs to the value.
        val result = parseProperty("ATTENDEE;ROLE=REQ-PARTICIPANT:mailto:john@example.com")
        assertEquals("ATTENDEE", result.name)
        assertEquals(mapOf("ROLE" to "REQ-PARTICIPANT"), result.params)
        assertEquals("mailto:john@example.com", result.value)
    }

    @Test
    fun parseProperty_quotedParamValueContainingColonAndSemicolon() {
        // Regression test for QUA-5: a naive split(":") / split(";") finds the delimiters
        // *inside* the quoted DELEGATED-FROM value and corrupts both the params and the
        // actual property value.
        val result = parseProperty(
            "ATTENDEE;CN=\"Doe, John\";DELEGATED-FROM=\"mailto:jane@example.com\":mailto:john@example.com"
        )
        assertEquals("ATTENDEE", result.name)
        assertEquals(
            mapOf(
                "CN" to "Doe, John",
                "DELEGATED-FROM" to "mailto:jane@example.com"
            ),
            result.params
        )
        assertEquals("mailto:john@example.com", result.value)
    }

    @Test
    fun parseProperty_quotedParamValueContainingSemicolon() {
        val result = parseProperty("X-PROP;PARAM1=\"a;b\";PARAM2=plain:value")
        assertEquals("X-PROP", result.name)
        assertEquals(mapOf("PARAM1" to "a;b", "PARAM2" to "plain"), result.params)
        assertEquals("value", result.value)
    }

    @Test
    fun parseProperty_unquotedValueWithCommas() {
        val result = parseProperty("RDATE;VALUE=DATE:20260101,20260102,20260103")
        assertEquals("RDATE", result.name)
        assertEquals(mapOf("VALUE" to "DATE"), result.params)
        assertEquals("20260101,20260102,20260103", result.value)
    }

    @Test
    fun parseProperty_valueWithoutColonDoesNotThrow() {
        val result = parseProperty("X-MALFORMED")
        assertEquals("X-MALFORMED", result.name)
        assertEquals(emptyMap(), result.params)
        assertEquals("", result.value)
    }
}
