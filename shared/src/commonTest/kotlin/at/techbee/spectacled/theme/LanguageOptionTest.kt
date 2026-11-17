package at.techbee.spectacled.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageOptionTest {

    @Test
    fun fromString_returnsMatchingEntry() {
        LanguageOption.entries.forEach { assertEquals(it, LanguageOption.fromString(it.name)) }
    }

    @Test
    fun fromString_fallsBackToSystem() {
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromString(null))
        assertEquals(LanguageOption.SYSTEM, LanguageOption.fromString("KLINGON"))
    }
}
