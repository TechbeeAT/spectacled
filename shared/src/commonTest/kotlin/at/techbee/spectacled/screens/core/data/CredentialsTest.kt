package at.techbee.spectacled.screens.core.data

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [Credentials.hasUsernameAndPassword] is what every WebDAV request checks before sending a
 * basicAuth header. Getting it wrong in either direction is costly: too eager and the app sends
 * `Basic base64(":")` to servers that would have served the request anonymously, too shy and it
 * drops the credentials the user actually entered.
 */
class CredentialsTest {

    private fun credentials(username: String, password: String) =
        Credentials(Url("https://caldav.example.com"), username, password)

    @Test
    fun bothPresent() {
        assertTrue(credentials("user", "secret").hasUsernameAndPassword())
    }

    @Test
    fun neitherPresent() {
        // An anonymous account - a read-only collection on a server that does not authenticate.
        assertFalse(credentials("", "").hasUsernameAndPassword())
    }

    @Test
    fun onlyOneOfThemPresent() {
        // Half a credential is not worth sending: a server that wants auth rejects it anyway, and
        // one that does not would have answered without it.
        assertFalse(credentials("user", "").hasUsernameAndPassword())
        assertFalse(credentials("", "secret").hasUsernameAndPassword())
    }

    @Test
    fun whitespaceOnlyCountsAsAbsent() {
        // isNotBlank rather than isNotEmpty, so a field holding only spaces is treated as empty.
        assertFalse(credentials("   ", "secret").hasUsernameAndPassword())
        assertFalse(credentials("user", "   ").hasUsernameAndPassword())
        assertFalse(credentials(" ", "\t").hasUsernameAndPassword())
    }

    @Test
    fun surroundingWhitespaceDoesNotMakeAValueAbsent() {
        // Only entirely blank counts: a password that happens to start or end with a space is real.
        assertTrue(credentials("user", " secret ").hasUsernameAndPassword())
    }
}
