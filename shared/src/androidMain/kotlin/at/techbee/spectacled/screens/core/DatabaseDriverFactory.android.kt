package at.techbee.spectacled.screens.core

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual class DatabaseDriverFactory(private val context: Context) {

    companion object {
        private var driver: SqlDriver? = null
        private val mutex = Mutex()
    }

    actual suspend fun provideDbDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {
        return mutex.withLock {
            driver ?: AndroidSqliteDriver(
                schema = schema.synchronous(),
                context = context,
                name = DATABASE_NAME,
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
            ).also { driver = it }
        }
    }
}