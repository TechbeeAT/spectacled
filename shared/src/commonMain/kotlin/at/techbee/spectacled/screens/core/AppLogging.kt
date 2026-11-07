package at.techbee.spectacled.screens.core

import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier

/**
 * Sets up logging once per process. Debug builds log everything (including the HTTP request
 * headers, see HttpClientFactory); release builds only log warnings and errors.
 */
object AppLogging {

    /** Whether this is a debug build, as passed to the first [init] call. */
    var isDebugBuild: Boolean = false
        private set

    private var isInitialized = false

    /**
     * Only the first call has an effect: Napier.base() adds an Antilog rather than replacing it, so
     * a second call would print every message twice. The Android apps call this from
     * Application.onCreate (where the debuggable flag is known) before doInitKoin calls it again.
     */
    fun init(isDebugBuild: Boolean) {
        if (isInitialized)
            return
        isInitialized = true
        this.isDebugBuild = isDebugBuild
        Napier.base(if (isDebugBuild) DebugAntilog() else MinLevelAntilog(DebugAntilog(), LogLevel.WARNING))
    }
}

/** Forwards only messages of [minLevel] or above to [delegate]. */
private class MinLevelAntilog(
    private val delegate: Antilog,
    private val minLevel: LogLevel
) : Antilog() {

    override fun isEnable(priority: LogLevel, tag: String?): Boolean = priority >= minLevel

    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) =
        delegate.log(priority, tag, throwable, message)
}

/**
 * Best guess whether this is a debug build, for platforms whose entry point doesn't pass it to
 * [AppLogging.init] itself.
 */
expect fun platformIsDebugBuild(): Boolean
