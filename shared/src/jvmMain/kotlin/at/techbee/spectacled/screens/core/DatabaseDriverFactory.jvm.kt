package at.techbee.spectacled.screens.core

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File


actual class DatabaseDriverFactory(val spectacledVariant: SpectacledVariant) {

    companion object {
        private var database: SpectacledDatabase? = null
        private val mutex = Mutex()
        private const val TAG = "DatabaseDriverFactory"
    }

    actual suspend fun provideDatabase(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SpectacledDatabase {
        return mutex.withLock {
            database ?: run {
                val dbFile = File(System.getProperty("user.home"), ".spectacled/${spectacledVariant.dbName}")
                dbFile.parentFile.mkdirs()

                val newDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
                val targetVersion = schema.version
                val currentVersion = readUserVersion(newDriver)

                when {
                    currentVersion == 0L && !hasExistingTables(newDriver) -> {
                        Napier.i(tag = TAG) { "Creating new Desktop database '${spectacledVariant.dbName}' at schema version $targetVersion" }
                        schema.synchronous().create(newDriver).await()
                        newDriver.execute(null, "PRAGMA user_version = $targetVersion;", 0)
                    }
                    currentVersion == 0L -> {
                        // Tables already exist but user_version was never set - this database was
                        // created by an older build that only ever ran schema.create() and never
                        // tracked a version (that's the exact bug this fix addresses). We can't
                        // know for certain which historical schema state it's structurally in, so
                        // rather than guessing at replaying old migrations (and risking a "table/
                        // column already exists" crash like schema.create() just did), we start
                        // tracking the version from here. This does not retroactively repair any
                        // schema drift such installs may already have, but every migration from
                        // this point forward will now apply correctly.
                        Napier.i(tag = TAG) { "Adopting version tracking for existing untracked Desktop database '${spectacledVariant.dbName}' at schema version $targetVersion" }
                        newDriver.execute(null, "PRAGMA user_version = $targetVersion;", 0)
                    }
                    currentVersion < targetVersion -> {
                        Napier.i(tag = TAG) { "Migrating Desktop database '${spectacledVariant.dbName}' from version $currentVersion to $targetVersion" }
                        schema.synchronous().migrate(newDriver, currentVersion, targetVersion).await()
                        newDriver.execute(null, "PRAGMA user_version = $targetVersion;", 0)
                    }
                    currentVersion > targetVersion -> {
                        // DB is newer than this app build, most likely a downgrade. Don't touch
                        // the schema; log loudly so it's visible if data looks wrong.
                        Napier.e(tag = TAG) { "Desktop database '${spectacledVariant.dbName}' is at version $currentVersion, newer than app schema version $targetVersion. Downgrade is not supported." }
                    }
                    // currentVersion == targetVersion -> already up to date, nothing to do.
                }

                // PRAGMA foreign_keys is a per-connection setting, not persisted in the DB file,
                // so it must be (re-)enabled on every open, not just on first creation.
                newDriver.execute(null, "PRAGMA foreign_keys=ON;", 0)

                SpectacledDatabase(newDriver).also { db ->
                    // 🔥 FORCE DB OPEN HERE
                    db.calendar_dtoQueries.getAllCalendars().executeAsList()
                    database = db
                }
            }
        }
    }

    private suspend fun readUserVersion(driver: SqlDriver): Long =
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version;",
            mapper = { cursor -> QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 0L else 0L) },
            parameters = 0,
        ).await()

    private suspend fun hasExistingTables(driver: SqlDriver): Boolean =
        driver.executeQuery(
            identifier = null,
            sql = "SELECT count(*) FROM sqlite_master WHERE type='table';",
            mapper = { cursor -> QueryResult.Value(if (cursor.next().value) (cursor.getLong(0) ?: 0L) > 0L else false) },
            parameters = 0,
        ).await()
}