package at.techbee.spectacled.screens.core

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import at.techbee.spectacled.SpectacledVariant


expect class DatabaseDriverFactory {
    suspend fun provideDbDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver
}
