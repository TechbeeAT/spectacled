package at.techbee.spectacled.screens.core

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import at.techbee.spectacled.SpectacledVariant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File


actual class DatabaseDriverFactory(val spectacledVariant: SpectacledVariant) {

    companion object {
        private var driver: SqlDriver? = null
        private val mutex = Mutex()
    }

    actual suspend fun provideDbDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {
        return mutex.withLock {
            driver ?: run {
                val dbFile = File(System.getProperty("user.home"), ".spectacled/${spectacledVariant.dbName}")
                val newDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

                if (!dbFile.exists()) {
                    dbFile.parentFile.mkdirs()
                    schema.synchronous().create(newDriver).await()
                    newDriver.execute(null, "PRAGMA foreign_keys=ON;", 0)
                }
                newDriver.also { driver = it }
            }
        }
    }
}