package at.techbee.spectacled.screens.core.data.webdav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DavErrorMessageTest {

    private val sabreError = """
        <?xml version="1.0" encoding="utf-8"?>
        <d:error xmlns:d="DAV:" xmlns:s="http://sabredav.org/ns">
          <s:exception>Sabre\DAV\Exception\UnsupportedMediaType</s:exception>
          <s:message>The VJOURNAL properties is not supported</s:message>
        </d:error>
    """.trimIndent()

    @Test
    fun extractDavErrorMessage_sabre() {
        assertEquals("The VJOURNAL properties is not supported", extractDavErrorMessage(sabreError))
    }

    @Test
    fun extractDavErrorMessage_unprefixedAndEscaped() {
        assertEquals("a <b> & \"c\"", extractDavErrorMessage("<error><message> a &lt;b&gt; &amp; &quot;c&quot; </message></error>"))
    }

    @Test
    fun extractDavErrorMessage_noMessage() {
        assertNull(extractDavErrorMessage(null))
        assertNull(extractDavErrorMessage(""))
        assertNull(extractDavErrorMessage("<d:error xmlns:d=\"DAV:\"><d:need-privileges/></d:error>"))
        assertNull(extractDavErrorMessage("<d:error xmlns:d=\"DAV:\"><s:message>  </s:message></d:error>"))
    }

    @Test
    fun withDavErrorMessage_appendsServerMessage() {
        assertEquals("Creating collection failed\nThe VJOURNAL properties is not supported", "Creating collection failed".appendDavErrorMessage(sabreError))
        assertEquals("Creating collection failed", "Creating collection failed".appendDavErrorMessage("Internal Server Error"))
    }
}
