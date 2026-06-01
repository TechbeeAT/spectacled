package at.techbee.spectacled.screens.core

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.db.SpectacledDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class DatabaseDriverFactory(
    private val context: Context,
    val spectacledVariant: SpectacledVariant
) {

    companion object {
        private val mutex = Mutex()
        private val databases = mutableMapOf<String, SpectacledDatabase>()
    }

    actual suspend fun provideDatabase(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SpectacledDatabase {
        return mutex.withLock {
            databases.getOrPut(spectacledVariant.dbName) {
                SpectacledDatabase(AndroidSqliteDriver(
                    schema = schema.synchronous(),
                    context = context.applicationContext,
                    name = spectacledVariant.dbName,
                    callback = object : AndroidSqliteDriver.Callback(schema.synchronous()) {

                        override fun onConfigure(db: SupportSQLiteDatabase) {
                            super.onConfigure(db)
                            db.setForeignKeyConstraintsEnabled(true)  // also enables cascading delete
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.enableWriteAheadLogging()
                        }
                    }
                )).also { db ->
                    // 🔥 FORCE DB OPEN HERE
                    db.calendar_dtoQueries.getAllCalendars().executeAsList()
                }
            }
        }
    }
}