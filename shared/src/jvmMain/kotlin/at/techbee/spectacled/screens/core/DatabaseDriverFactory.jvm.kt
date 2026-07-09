package at.techbee.spectacled.screens.core

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File


actual class DatabaseDriverFactory(val spectacledVariant: SpectacledVariant) {

    companion object {
        private var database: SpectacledDatabase? = null
        private val mutex = Mutex()
    }

    actual suspend fun provideDatabase(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SpectacledDatabase {
        return mutex.withLock {
            database ?: run {
                val dbFile = File(System.getProperty("user.home"), ".spectacled/${spectacledVariant.dbName}")
                dbFile.parentFile.mkdirs()

                val newDriver = JdbcSqliteDriver(
                    url = "jdbc:sqlite:${dbFile.absolutePath}",
                    schema = schema.synchronous(),
                )

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
}