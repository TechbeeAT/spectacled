package at.techbee.spectacled.screens.core.data.webdav

import kotlinx.io.IOException

/**
 * True if this exception (or one of its causes) means the server couldn't be reached at all,
 * e.g. no internet connection, an unresolvable host or a connect timeout. All Ktor engines we use
 * (OkHttp, Darwin) report these as [IOException], including Ktor's own timeout exceptions.
 */
fun Throwable.isNetworkError(): Boolean =
    generateSequence(this) { it.cause }.any { it is IOException }
