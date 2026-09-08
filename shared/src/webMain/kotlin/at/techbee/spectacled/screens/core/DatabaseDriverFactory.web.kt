package at.techbee.spectacled.screens.core

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.data.LocalDataPersistence
import at.techbee.spectacled.screens.core.data.WebLocalDataPolicy
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.w3c.dom.Worker


@OptIn(ExperimentalWasmJsInterop::class)
fun jsWorker(sessionOnly: Boolean, wipeStoredDatabase: Boolean): Worker =
    // spectacledSqlWorker.js is our own copy of @cashapp/sqldelight-sqljs-worker's
    // sqljs.worker.js with IndexedDB persistence added. It lives in each
    // compose*App's src/webMain/resources (same place as favicon.ico/index.html/
    // styles.css), which the Kotlin Gradle plugin copies to the web root for both the
    // js() and wasmJs() browser targets automatically - shared/src/webMain/resources
    // does NOT get bundled into the final app the same way, so it has to live in the
    // app module, not here, even though this factory itself is shared code.
    // Relative (no leading slash) so it resolves against the page's base URL and works whether the
    // app is served at the site root or under a subpath (e.g. /journals/ on GitHub Pages).
    //
    // What the worker does with its stored snapshot travels in the query string, because it has to
    // be settled before the worker opens anything - and js(...) only takes a compile-time constant,
    // hence the four spelled-out URLs instead of one built from the two flags.
    when {
        sessionOnly && wipeStoredDatabase -> js("""new Worker("spectacledSqlWorker.js?sessionOnly=1&wipe=1")""")
        sessionOnly -> js("""new Worker("spectacledSqlWorker.js?sessionOnly=1")""")
        wipeStoredDatabase -> js("""new Worker("spectacledSqlWorker.js?wipe=1")""")
        else -> js("""new Worker("spectacledSqlWorker.js")""")
    }

actual class DatabaseDriverFactory {

    companion object {
        private var database: SpectacledDatabase? = null
        private val mutex = Mutex()
    }

    actual suspend fun provideDatabase(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SpectacledDatabase {

        return mutex.withLock {
            database ?: run {
                // In a private session the worker keeps the database in memory, so the schema is
                // created from scratch on every start - readUserVersion below returns 0 and takes
                // care of that without any special casing here.
                val sessionOnly = WebLocalDataPolicy.current == LocalDataPersistence.SESSION_ONLY

                Napier.d("Creating WebWorker Driver (sessionOnly=$sessionOnly)")
                val d = WebWorkerDriver(jsWorker(sessionOnly, WebLocalDataPolicy.consumePendingWipe()))

                // spectacledSqlWorker.js now restores the database from IndexedDB on startup
                // (see DAT-6), so this can no longer unconditionally call schema.create() -
                // that would try to re-create tables that already exist on every reload after
                // the first one. Track the schema version the same way Android/iOS/Desktop do.
                val targetVersion = schema.version
                val currentVersion = readUserVersion(d)

                if (currentVersion == 0L) {
                    Napier.d("Creating database schema at version $targetVersion")
                    schema.create(d).await()
                    d.execute(null, "PRAGMA user_version = $targetVersion;", 0).await()
                } else if (currentVersion < targetVersion) {
                    Napier.d("Migrating database from version $currentVersion to $targetVersion")
                    schema.migrate(d, currentVersion, targetVersion).await()
                    d.execute(null, "PRAGMA user_version = $targetVersion;", 0).await()
                } else {
                    Napier.d("Database already at version $currentVersion")
                }

                Napier.d("Enabling foreign keys")
                d.execute(null, "PRAGMA foreign_keys=ON;", 0).await()

                Napier.d("Driver fully initialized")
                SpectacledDatabase(d).also { database = it }
            }
        }
    }

    private suspend fun readUserVersion(driver: SqlDriver): Long =
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor -> QueryResult.AsyncValue { if (cursor.next().await()) cursor.getLong(0) ?: 0L else 0L } },
            parameters = 0,
        ).await()
}
