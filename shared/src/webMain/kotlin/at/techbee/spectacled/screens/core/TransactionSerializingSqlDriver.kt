package at.techbee.spectacled.screens.core

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Wraps the web's `WebWorkerDriver` so that only one coroutine at a time can use the database while
 * a transaction is open.
 *
 * `WebWorkerDriver` keeps a single, driver-wide "current transaction". On the web every statement
 * suspends until the worker answers, so two coroutines can have a transaction open at the same
 * time (e.g. two calendars syncing in parallel). The second one is then treated as nested in the
 * first, and once they end in the wrong order `currentTransaction()` keeps returning a transaction
 * that has already ended. From then on SQLDelight queues every table notification on that dead
 * transaction instead of firing it, so no `asFlow()` query re-emits until the page is reloaded (a
 * sync never visibly finished). Besides, all statements share the worker's single connection, so
 * another coroutine's writes land in the open SQL transaction and are lost if it rolls back.
 *
 * This driver therefore runs transactions itself (BEGIN/END/ROLLBACK through [delegate]'s
 * `execute`, its own transaction handling is never used) and holds [lock] from the outermost BEGIN
 * until its END or ROLLBACK. Statements and nested transactions of the coroutine that opened the
 * transaction - or of a child coroutine of it, e.g. `withContext` inside the transaction body -
 * pass straight through; every other coroutine waits until the transaction has ended.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionSerializingSqlDriver(private val delegate: SqlDriver) : SqlDriver {

    private val lock = Mutex()
    private var transaction: SerializedTransaction? = null
    private var transactionOwner: Job? = null

    /** every query key a listener was ever registered for, see [SerializedTransaction.endTransaction] */
    private val knownQueryKeys = mutableSetOf<String>()

    private suspend fun isTransactionOwner(): Boolean {
        val owner = transactionOwner ?: return false
        var job = currentCoroutineContext()[Job]
        while (job != null) {
            if (job === owner)
                return true
            job = job.parent
        }
        return false
    }

    private suspend fun <T> serialized(block: suspend () -> T): T =
        if (isTransactionOwner()) block() else lock.withLock { block() }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> = QueryResult.AsyncValue {
        serialized { delegate.executeQuery(identifier, sql, mapper, parameters, binders).await() }
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> = QueryResult.AsyncValue {
        serialized { delegate.execute(identifier, sql, parameters, binders).await() }
    }

    override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.AsyncValue {
        if (isTransactionOwner())
            return@AsyncValue SerializedTransaction(enclosingTransaction = transaction).also { transaction = it }

        val job = checkNotNull(currentCoroutineContext()[Job]) {
            "Database transactions must be started from a coroutine with a Job"
        }
        lock.lock()
        try {
            withContext(NonCancellable) { delegate.execute(null, "BEGIN TRANSACTION;", 0).await() }
        } catch (e: Throwable) {
            lock.unlock()
            throw e
        }
        transactionOwner = job
        SerializedTransaction(enclosingTransaction = null).also { transaction = it }
    }

    override fun currentTransaction(): Transacter.Transaction? = transaction

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
        knownQueryKeys.addAll(queryKeys)
        delegate.addListener(*queryKeys, listener = listener)
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) =
        delegate.removeListener(*queryKeys, listener = listener)

    override fun notifyListeners(vararg queryKeys: String) =
        delegate.notifyListeners(*queryKeys)

    override fun close() = delegate.close()

    private inner class SerializedTransaction(
        override val enclosingTransaction: SerializedTransaction?,
    ) : Transacter.Transaction() {

        override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.AsyncValue {
            if (enclosingTransaction != null) {
                transaction = enclosingTransaction
                return@AsyncValue
            }

            // NonCancellable: the Transacter ends a transaction in a finally block, also when the
            // coroutine was cancelled. END/ROLLBACK must still reach the worker, otherwise the SQL
            // transaction stays open after the lock is released.
            try {
                withContext(NonCancellable) {
                    if (successful) {
                        try {
                            delegate.execute(null, "END TRANSACTION;", 0).await()
                        } catch (e: Throwable) {
                            runCatching { delegate.execute(null, "ROLLBACK TRANSACTION;", 0).await() }
                            throw e
                        }
                    } else {
                        delegate.execute(null, "ROLLBACK TRANSACTION;", 0).await()
                    }
                }
            } finally {
                transaction = null
                transactionOwner = null
                lock.unlock()
            }

            // SQLDelight drops the pending notifications of a rolled back transaction. A write of
            // another coroutine can resume right after a transaction began and queue its
            // notification on it, so notify everything rather than lose that one.
            if (!successful)
                delegate.notifyListeners(*knownQueryKeys.toTypedArray())
        }
    }
}
