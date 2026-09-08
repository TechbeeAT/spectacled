package at.techbee.spectacled.screens.core.data

/**
 * How long the local copy of the connected accounts and their entries is allowed to live.
 *
 * Only the web build really has a choice here. Android, iOS and Desktop are installed on a device
 * that belongs to whoever installed them, so keeping everything until the account is removed is the
 * only sensible behaviour. The web app may just as well be running on a library or hotel computer,
 * where a browser that hands the next visitor a ready-to-use account is exactly what must not
 * happen.
 */
enum class LocalDataPersistence {

    /** Credentials, settings and the local database survive the app being closed. The default. */
    PERSISTENT,

    /**
     * Nothing is written to the browser: credentials, settings and the whole database live in
     * memory only, so closing or reloading the tab leaves nothing behind.
     */
    SESSION_ONLY
}


/**
 * Reads and changes the [LocalDataPersistence] the app runs under.
 *
 * The mode is fixed for the lifetime of the process, and changing it therefore restarts the app.
 * That is not laziness: the database worker decides when it starts whether it restores and writes
 * its IndexedDB snapshot, and both KSafe-backed stores decide when they are constructed whether
 * they write to the browser at all. Flipping a flag under the running app would leave at least one
 * of them still writing to disk while the UI claims the session is private.
 */
interface LocalDataPolicy {

    /** `true` only where [restartWith] does anything, i.e. on the web build. */
    val isConfigurable: Boolean

    /** The mode the app is running under right now. Constant for the lifetime of the process. */
    val current: LocalDataPersistence

    /**
     * Whether a restart can be carried out at all. Checked before anything is deleted, so that a
     * browser which refuses to remember the new mode costs the user nothing.
     */
    val canRestart: Boolean

    /**
     * Restarts the app so it runs under [mode], leaving whatever is already stored on this device
     * untouched: a private session hides the persistent account rather than deleting it, and
     * ending the session brings it back. Use [wipeAndRestart] to actually get rid of it.
     *
     * @return `false` if the mode could not be recorded, in which case nothing happened at all and
     *   the caller has to say so - silently continuing in the persistent mode after the user asked
     *   for a private session is the one outcome this must never produce.
     */
    fun restartWith(mode: LocalDataPersistence): Boolean

    /**
     * Removes everything this device holds locally - credentials, preferences and the database -
     * and restarts under [mode].
     *
     * This reaches past whatever the running app can see: in a private session the stores in use
     * are the in-memory ones, yet the account a previous visitor left in the browser is exactly
     * what "delete all data" has to get rid of. The database is dropped by the restart rather than
     * here, because the worker that owns it can delete it before it opens anything and so cannot
     * race its own pending snapshot write.
     */
    suspend fun wipeAndRestart(mode: LocalDataPersistence): Boolean
}


/** The policy of the installed apps: they keep their data, and there is nothing to restart into. */
object DeviceLocalDataPolicy : LocalDataPolicy {
    override val isConfigurable = false
    override val current = LocalDataPersistence.PERSISTENT
    override val canRestart = false
    override fun restartWith(mode: LocalDataPersistence) = false
    override suspend fun wipeAndRestart(mode: LocalDataPersistence) = false
}


expect fun getLocalDataPolicy(): LocalDataPolicy
