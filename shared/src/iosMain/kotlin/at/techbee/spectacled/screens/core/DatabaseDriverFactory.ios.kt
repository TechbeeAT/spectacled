package at.techbee.spectacled.screens.core

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


actual class DatabaseDriverFactory(val spectacledVariant: SpectacledVariant) {

    companion object {
        private var database: SpectacledDatabase? = null
        private val mutex = Mutex()
    }

    actual suspend fun provideDatabase(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SpectacledDatabase {
        return mutex.withLock {
            database ?: SpectacledDatabase(NativeSqliteDriver(schema.synchronous(), spectacledVariant.dbName).also {
                it.execute(null, "PRAGMA foreign_keys=ON;", 0)
            })
        }
    }
}