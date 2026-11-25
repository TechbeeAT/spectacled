package at.techbee.spectacled.screens.core

import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Behaves like WebWorkerDriver towards its caller: every statement suspends until the "worker"
 * answers, so coroutines interleave between statements. It logs the SQL it receives. Its own
 * transaction handling must never be used, the wrapper has to run transactions itself.
 */
private class SuspendingFakeDriver : SqlDriver {
    val log = mutableListOf<String>()
    private val listeners = mutableMapOf<String, MutableSet<Query.Listener>>()

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> = error("not needed")

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<Long> = QueryResult.AsyncValue {
        yield()
        log += sql
        yield()
        1L
    }

    override fun newTransaction(): QueryResult<Transacter.Transaction> = error("the delegate's transactions must not be used")
    override fun currentTransaction(): Transacter.Transaction? = error("the delegate's transactions must not be used")

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
        queryKeys.forEach { listeners.getOrPut(it) { mutableSetOf() }.add(listener) }
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
        queryKeys.forEach { listeners[it]?.remove(listener) }
    }

    override fun notifyListeners(vararg queryKeys: String) {
        queryKeys.flatMap { listeners[it].orEmpty() }.distinct().forEach(Query.Listener::queryResultsChanged)
    }

    override fun close() {}
}

/** Mirrors what SQLDelight generates for an async mutator: execute, then notify the table's queries. */
private class FakeQueries(driver: SqlDriver) : SuspendingTransacterImpl(driver) {
    suspend fun insert(table: String, value: String) {
        driver.execute(null, "INSERT $table $value", 0).await()
        notifyQueries(table.hashCode()) { emit -> emit(table) }
    }
}

private class CountingListener : Query.Listener {
    var count = 0
    override fun queryResultsChanged() { count++ }
}

class TransactionSerializingSqlDriverTest {

    private val fakeDriver = SuspendingFakeDriver()
    private val driver = TransactionSerializingSqlDriver(fakeDriver)
    private val queries = FakeQueries(driver)

    /** The bug behind the never-ending sync spinner on the web: overlapping transactions broke all later notifications. */
    @Test
    fun overlappingTransactions_laterWritesStillNotifyListeners() = runTest {
        val calendarListener = CountingListener()
        val entryListener = CountingListener()
        driver.addListener("CalendarDto", listener = calendarListener)
        driver.addListener("IcalEntryDto", listener = entryListener)

        (1..3).map { calendar ->
            async {
                repeat(3) { entry ->
                    queries.transaction {
                        queries.insert("IcalEntryDto", "$calendar-$entry-a")
                        queries.insert("IcalEntryDto", "$calendar-$entry-b")
                    }
                }
            }
        }.awaitAll()

        assertNull(driver.currentTransaction())
        assertEquals(9, entryListener.count)

        queries.insert("CalendarDto", "SYNCED")
        assertEquals(1, calendarListener.count)
    }

    @Test
    fun concurrentTransactions_runOneAfterTheOther() = runTest {
        listOf(
            launch { queries.transaction { repeat(3) { queries.insert("T", "a$it") } } },
            launch { queries.transaction { repeat(3) { queries.insert("T", "b$it") } } },
        ).forEach { it.join() }

        fun transactionOf(name: String) =
            listOf("BEGIN TRANSACTION;") + (0..2).map { "INSERT T $name$it" } + "END TRANSACTION;"
        assertEquals(transactionOf("a") + transactionOf("b"), fakeDriver.log)
    }

    @Test
    fun nestedTransactionAndChildCoroutine_ofTheOwner_passThrough() = runTest {
        val listener = CountingListener()
        driver.addListener("T", listener = listener)

        queries.transaction {
            queries.insert("T", "outer")
            withContext(Dispatchers.Default) { queries.insert("T", "child") }
            queries.transaction { queries.insert("T", "nested") }
        }

        assertEquals(
            listOf("BEGIN TRANSACTION;", "INSERT T outer", "INSERT T child", "INSERT T nested", "END TRANSACTION;"),
            fakeDriver.log
        )
        assertEquals(1, listener.count)
        assertNull(driver.currentTransaction())
    }

    @Test
    fun failedTransaction_rollsBackAndReleasesTheLock() = runTest {
        val listener = CountingListener()
        driver.addListener("T", listener = listener)

        assertFailsWith<IllegalStateException> {
            queries.transaction {
                queries.insert("T", "x")
                error("failure inside the transaction")
            }
        }
        assertEquals("ROLLBACK TRANSACTION;", fakeDriver.log.last())
        assertNull(driver.currentTransaction())
        assertEquals(0, listener.count)

        queries.transaction { queries.insert("T", "y") }
        assertEquals("END TRANSACTION;", fakeDriver.log.last())
        assertEquals(1, listener.count)
    }
}
