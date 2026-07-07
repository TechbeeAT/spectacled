package at.techbee.spectacled.screens.core.data.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.DatabaseDriverFactory
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.mapper.dto.toDomain
import at.techbee.spectacled.screens.core.mapper.dto.toDto
import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarRepositoryImpl(
    private val databaseDriverFactory: DatabaseDriverFactory
) : CalendarRepository {

    private suspend fun getDatabase() = databaseDriverFactory.provideDatabase(SpectacledDatabase.Schema)

    private val dbFlow = flow {
        emit(getDatabase())
    }

    override fun getAllPrincipalsFlow(): Flow<List<Principal>> {
        return dbFlow.flatMapLatest { db ->
            db.principal_dtoQueries.getAllPrincipals().asFlow()
                .map { query -> query.awaitAsList().map { it.toDomain() } }
        }
    }

    override fun getAllHomeCollectionsFlow(): Flow<List<HomeCollection>> {
        return dbFlow.flatMapLatest { db ->
            db.home_collection_dtoQueries.getAllHomeCollections().asFlow()
                .map { query -> query.awaitAsList().map { it.toDomain() } }
        }
    }

    override fun getAllCalendarsFlow(): Flow<List<Calendar>> {
        return dbFlow.flatMapLatest { db ->
            db.calendar_dtoQueries.getAllCalendars().asFlow()
                .map { query -> query.awaitAsList().map { it.toDomain() } }
        }
    }

    override suspend fun getAllPrincipals(): List<Principal> {
        return getDatabase().principal_dtoQueries.getAllPrincipals().awaitAsList().map { it.toDomain() }
    }

    override suspend fun getAllHomeCollections(): List<HomeCollection> {
        return getDatabase().home_collection_dtoQueries.getAllHomeCollections().awaitAsList().map { it.toDomain() }
    }

    override suspend fun getAllCalendars(): List<Calendar> {
        return getDatabase().calendar_dtoQueries.getAllCalendars().awaitAsList().map { it.toDomain() }
    }

    override suspend fun getCalendarById(id: Long): Calendar? {
        return getDatabase().calendar_dtoQueries.getCalendarById(id).awaitAsOneOrNull()?.toDomain()
    }

    override suspend fun getPrincipalForCalendar(calendarId: Long): Principal? {
        return getDatabase().principal_dtoQueries.getPrincipalForCalendar(calendarId).awaitAsOneOrNull()?.toDomain()
    }

    override suspend fun getPrincipalUrlForCalendarId(calendarId: Long): String? {
        return getDatabase().calendar_dtoQueries.getPrincipalUrlForCalendarId(calendarId).awaitAsOneOrNull()
    }

    override suspend fun getCalendarsForPrincipalUrl(principalUrl: String): List<Calendar> {
        return getDatabase().calendar_dtoQueries.getCalendarsForPrincipalUrl(principalUrl).awaitAsList().map { it.toDomain() }
    }

    override suspend fun getCalendarsByIds(calendarIds: List<Long>): List<Calendar> {
        return getDatabase().calendar_dtoQueries.getCalendarsByIds(calendarIds).awaitAsList().map { it.toDomain() }
    }

    override suspend fun upsertPrincipal(principal: Principal): Long {

        val principalDto = principal.toDto()
        val db = getDatabase()

        db.principal_dtoQueries.transaction {
            // first update, if the entry doesn't exist, this is ignored
            db.principal_dtoQueries.updatePrincipal(
                principalUrl = principalDto.principalUrl,
                displayName = principalDto.displayName,
                calendarUserAddressSet = principalDto.calendarUserAddressSet
            )
            // insert, but if the entry exists, it will be ignored
            db.principal_dtoQueries.insertPrincipal(
                principalUrl = principalDto.principalUrl,
                displayName = principalDto.displayName,
                calendarUserAddressSet = principalDto.calendarUserAddressSet
            )
        }

        return db.principal_dtoQueries.getPrincipalByUrl(principalDto.principalUrl).awaitAsOne().id
    }

    override suspend fun upsertHomeCollection(homeCollection: HomeCollection, principalUrl: Url): Long {
        val homeCollectionDto = homeCollection.toDto()

        getDatabase().home_collection_dtoQueries.transaction {
            // first update, if the entry doesn't exist, this is ignored
            getDatabase().home_collection_dtoQueries.updateHomeCollection(
                principalUrl = principalUrl.toString(),
                url = homeCollectionDto.url,
                calDavPrivileges = homeCollectionDto.calDavPrivileges,
                attachmentCollectionUrl = homeCollectionDto.attachmentCollectionUrl
            )
            // insert, but if the entry exists, it will be ignored
            getDatabase().home_collection_dtoQueries.insertHomeCollection(
                principalUrl = principalUrl.toString(),
                url = homeCollectionDto.url,
                calDavPrivileges = homeCollectionDto.calDavPrivileges,
                attachmentCollectionUrl = homeCollectionDto.attachmentCollectionUrl
            )
        }

        return getDatabase().home_collection_dtoQueries.getHomeCollectionsByUrl(homeCollectionDto.url).awaitAsOne().id
    }

    override suspend fun upsertCalendar(calendar: Calendar, homeCollectionUrl: Url): Long {
        val calendarDto = calendar.toDto()

        getDatabase().calendar_dtoQueries.transaction {

            // first update, if the entry doesn't exist, this is ignored
            getDatabase().calendar_dtoQueries.updateCalendar(
                homeCollectionUrl = homeCollectionUrl.toString(),
                url = calendarDto.url,
                displayName = calendarDto.displayName,
                calendarDescription = calendarDto.calendarDescription,
                color = calendarDto.color,
                ctag = calendarDto.ctag,
                supportedComponents = calendarDto.supportedComponents,
                calDavPrivileges = calendarDto.calDavPrivileges,
                calendarSyncStatus = calendarDto.calendarSyncStatus,
                syncToken = calendarDto.syncToken,
                attachmentCollectionUrl = calendarDto.attachmentCollectionUrl
            )
            // insert, but if the entry exists, it will be ignored
            getDatabase().calendar_dtoQueries.insertCalendar(
                homeCollectionUrl = homeCollectionUrl.toString(),
                url = calendarDto.url,
                displayName = calendarDto.displayName,
                calendarDescription = calendarDto.calendarDescription,
                color = calendarDto.color,
                ctag = calendarDto.ctag,
                supportedComponents = calendarDto.supportedComponents,
                calDavPrivileges = calendarDto.calDavPrivileges,
                calendarSyncStatus = calendarDto.calendarSyncStatus,
                syncToken = calendarDto.syncToken,
                attachmentCollectionUrl = calendarDto.attachmentCollectionUrl
            )
        }

        return getDatabase().calendar_dtoQueries.getCalendarByUrl(calendarDto.url).awaitAsOne().id
    }

    override suspend fun deletePrincipal(id: Long) {
        getDatabase().principal_dtoQueries.delete(id)
    }

    override suspend fun deleteCalendar(id: Long) {
        getDatabase().calendar_dtoQueries.delete(id)
    }

    override suspend fun updateCalendarSyncStatus(calendarSyncStatus: String?, syncToken: String?, id: Long) {
        getDatabase().calendar_dtoQueries.updateCalendarSyncStatus(calendarSyncStatus, syncToken, id)
    }
}
