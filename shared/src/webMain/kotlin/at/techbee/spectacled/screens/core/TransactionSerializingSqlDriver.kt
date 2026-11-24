package at.techbee.spectacled.screens.core

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * Wraps the web's `WebWorkerDriver` so that transactions of different coroutines run one after the
 * other instead of overlapping.
 *
 * `WebWorkerDriver` keeps a single, driver-wide "current transaction". On the web every statement
 * suspends until the worker answers, so two coroutines can have a transaction open at the same
 * time (e.g. two calendars syncing in parallel). The second one is then treated as nested in the
 * first, and once they end in the wrong order `currentTransaction()` keeps returning a transaction
 * that has already ended. From then on SQLDelight queues every table notification on that dead
 * transaction instead of firing it, so no `asFlow()` query re-emits until the page is reloaded (a
 * sync never visibly finished).
 *
 * Only the transaction handling is replaced, everything else goes straight to [delegate]: a
 * transaction holds [lock] from its BEGIN until its END or ROLLBACK, so the next one has to wait.
 * A transaction started inside another one by the same coroutine (or a child coroutine of it) is
 * nested as usual.
 *
 * Single statements outside a transaction are not held back. As all statements share the worker's
 * one connection, such a write of another coroutine can still end up in an open transaction and is
 * then undone with it, should that transaction roll back.
 */
class TransactionSerializingSqlDriver(private val delegate: SqlDriver) : SqlDriver by delegate {

    private val lock = Mutex()
    private var transaction: SerializedTransaction? = null
    private var transactionOwner: Job? = null

    override fun currentTransaction(): Transacter.Transaction? = transaction

    override fun newTransaction(): QueryResult<Transacter.Transaction> = QueryResult.AsyncValue {
        if (isTransactionOwner())
            return@AsyncValue SerializedTransaction(enclosingTransaction = transaction).also { transaction = it }

        lock.lock()
        try {
            // NonCancellable (also below): once the statement was posted the worker executes it,
            // so the result has to be awaited, or lock and SQL transaction would get out of step
            withContext(NonCancellable) { delegate.execute(null, "BEGIN TRANSACTION;", 0).await() }
        } catch (e: Throwable) {
            lock.unlock()
            throw e
        }
        transactionOwner = currentCoroutineContext()[Job]
        SerializedTransaction(enclosingTransaction = null).also { transaction = it }
    }

    /** true if the calling coroutine, or one of its parents, opened the current transaction */
    @OptIn(ExperimentalCoroutinesApi::class)
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

    private inner class SerializedTransaction(
        override val enclosingTransaction: SerializedTransaction?,
    ) : Transacter.Transaction() {

        override fun endTransaction(successful: Boolean): QueryResult<Unit> = QueryResult.AsyncValue {
            if (enclosingTransaction != null) {
                transaction = enclosingTransaction
                return@AsyncValue
            }

            try {
                withContext(NonCancellable) {
                    delegate.execute(null, if (successful) "END TRANSACTION;" else "ROLLBACK TRANSACTION;", 0).await()
                }
            } finally {
                transaction = null
                transactionOwner = null
                lock.unlock()
            }
        }
    }
}
