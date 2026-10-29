package at.techbee.spectacled.screens.core.data.webdav

import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkErrorTest {

    @Test
    fun isNetworkError_ioException() {
        assertTrue(IOException("Unable to resolve host \"example.com\"").isNetworkError())
    }

    @Test
    fun isNetworkError_wrappedIoException() {
        assertTrue(IllegalStateException("wrapper", IOException("offline")).isNetworkError())
    }

    @Test
    fun isNetworkError_otherException() {
        assertFalse(IllegalStateException("something else").isNetworkError())
    }
}
