package at.techbee.spectacled.screens.core

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.w3c.dom.Worker


@OptIn(ExperimentalWasmJsInterop::class)
fun jsWorker(): Worker =
    js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")

actual class DatabaseDriverFactory {

    companion object {
        private var driver: SqlDriver? = null
        private val mutex = Mutex()
    }

    actual suspend fun provideDbDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver {

        return mutex.withLock {
            driver ?: run {
                Napier.d("Creating WebWorker Driver")
                val d = WebWorkerDriver(jsWorker())
                Napier.d("Awaiting schema creation")
                schema.create(d).await()
                Napier.d("Enabling foreign keys")
                d.execute(null, "PRAGMA foreign_keys=ON;", 0)

                Napier.d("Driver fully initialized")
                driver = d
                d
            }
        }
    }
}