package at.techbee.spectacled.screens.core.domain.repository

import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import io.ktor.http.Url
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {

    fun getAllPrincipalsFlow(): Flow<List<Principal>>
    fun getAllHomeCollectionsFlow(): Flow<List<HomeCollection>>
    fun getAllCalendarsFlow(): Flow<List<Calendar>>

    suspend fun getAllPrincipals(): List<Principal>
    suspend fun getAllHomeCollections(): List<HomeCollection>
    suspend fun getAllCalendars(): List<Calendar>

    suspend fun getCalendarById(id: Long): Calendar?
    suspend fun getPrincipalForCalendar(calendarId: Long): Principal?
    suspend fun getPrincipalUrlForCalendarId(calendarId: Long): String?
    suspend fun getCalendarsForPrincipalUrl(principalUrl: String): List<Calendar>
    suspend fun getCalendarsByIds(calendarIds: List<Long>): List<Calendar>
    
    suspend fun upsertPrincipal(principal: Principal): Long
    suspend fun upsertHomeCollection(homeCollection: HomeCollection, principalUrl: Url): Long
    suspend fun upsertCalendar(calendar: Calendar, homeCollectionUrl: Url): Long
    
    suspend fun deletePrincipal(id: Long)
    suspend fun deleteCalendar(id: Long)
    suspend fun updateCalendarSyncStatus(calendarSyncStatus: String?, syncToken: String?, id: Long)
}
