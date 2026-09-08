package at.techbee.spectacled.screens.core.data

import eu.anifantakis.lib.ksafe.KSafe
import io.github.aakira.napier.Napier
import kotlinx.browser.window

/**
 * Where the chosen mode is remembered. Session storage on purpose: it is scoped to this one tab and
 * dies with it, so a private session can neither outlive the tab that started it nor greet the next
 * visitor of a browser that was left in one.
 */
private const val PERSISTENCE_MODE_KEY = "spectacled.localDataPersistence"
private const val SESSION_ONLY_VALUE = "session-only"

/** Set right before a restart to tell the next start to drop the stored database. */
private const val PENDING_WIPE_KEY = "spectacled.pendingDatabaseWipe"

/** Written and read back once to find out whether this browser lets the app store anything at all. */
private const val STORAGE_PROBE_KEY = "spectacled.sessionStorageProbe"


object WebLocalDataPolicy : LocalDataPolicy {

    override val isConfigurable = true

    /**
     * Resolved once and then cached, so the credential store, the preferences store and the
     * database worker cannot end up disagreeing about the mode halfway through a page's life.
     */
    override val current: LocalDataPersistence by lazy {
        if (readItem(PERSISTENCE_MODE_KEY) == SESSION_ONLY_VALUE) LocalDataPersistence.SESSION_ONLY
        else LocalDataPersistence.PERSISTENT
    }

    override val canRestart: Boolean
        get() = isSessionStorageUsable()

    override fun restartWith(mode: LocalDataPersistence): Boolean {
        if (!canRestart) return false
        writeMode(mode)
        window.location.reload()
        return true
    }

    override suspend fun wipeAndRestart(mode: LocalDataPersistence): Boolean {
        if (!canRestart) return false

        // Deliberately fresh KSafe instances rather than the ones Koin hands the app: in a private
        // session those write to memory, and it is precisely the browser's own storage - possibly
        // holding an account somebody else left behind - that has to be emptied here. clearAll()
        // only touches the keys of the store it is called on (KSafe prefixes them), so a sibling
        // app served from the same origin keeps its own.
        KSafe(CREDENTIALS_FILE_NAME).clearAll()

        // The proxy setting goes with everything else. It survives a private session, where nothing
        // was asked to be deleted, but "delete all data in this browser" that quietly keeps a
        // setting is not what it says on the button.
        KSafe(APP_PREFERENCES_FILE_NAME).clearAll()

        writeMode(mode)
        writeItem(PENDING_WIPE_KEY, "true")
        window.location.reload()
        return true
    }

    /**
     * Whether this start should delete the stored database. Clears the request on the way out, so a
     * later reload of the same tab keeps whatever the app writes from here on.
     */
    fun consumePendingWipe(): Boolean {
        val pending = readItem(PENDING_WIPE_KEY) != null
        if (pending) removeItem(PENDING_WIPE_KEY)
        return pending
    }

    private fun writeMode(mode: LocalDataPersistence) {
        if (mode == LocalDataPersistence.SESSION_ONLY) writeItem(PERSISTENCE_MODE_KEY, SESSION_ONLY_VALUE)
        else removeItem(PERSISTENCE_MODE_KEY)
    }

    /**
     * Probed rather than assumed: session storage can be there and still refuse to keep anything (a
     * sandboxed frame, a browser set to block site data). Since the mode only takes effect after the
     * reload, a write that quietly does nothing would restart the app in the persistent mode while
     * the user believes they are in a private session.
     */
    private fun isSessionStorageUsable(): Boolean {
        writeItem(STORAGE_PROBE_KEY, "1")
        val usable = readItem(STORAGE_PROBE_KEY) == "1"
        removeItem(STORAGE_PROBE_KEY)
        if (!usable) Napier.w("This browser does not allow session storage, the local data mode cannot be changed")
        return usable
    }

    private fun readItem(key: String): String? = runCatching { window.sessionStorage.getItem(key) }.getOrNull()
    private fun writeItem(key: String, value: String) { runCatching { window.sessionStorage.setItem(key, value) } }
    private fun removeItem(key: String) { runCatching { window.sessionStorage.removeItem(key) } }
}


actual fun getLocalDataPolicy(): LocalDataPolicy = WebLocalDataPolicy
