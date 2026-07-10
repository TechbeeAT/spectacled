package at.techbee.spectacled.screens.core

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import at.techbee.spectacled.db.SpectacledDatabase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.w3c.dom.Worker


@OptIn(ExperimentalWasmJsInterop::class)
fun jsWorker(): Worker =
    // spectacledSqlWorker.js is our own copy of @cashapp/sqldelight-sqljs-worker's
    // sqljs.worker.js with IndexedDB persistence added (see DAT-6). It lives in
    // shared/src/webMain/resources, which the Kotlin Gradle plugin copies to the web
    // root for both the js() and wasmJs() browser targets automatically, so it's loaded
    // as a plain static asset rather than resolved through the npm package.
    js("""new Worker("/spectacledSqlWorker.js")""")

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
                Napier.d("Creating WebWorker Driver")
                val d = WebWorkerDriver(jsWorker())

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