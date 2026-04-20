package at.techbee.spectacled.screens.core

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlSchema
import at.techbee.spectacled.db.SpectacledDatabase


expect class DatabaseDriverFactory {
    suspend fun provideDatabase(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SpectacledDatabase
}
