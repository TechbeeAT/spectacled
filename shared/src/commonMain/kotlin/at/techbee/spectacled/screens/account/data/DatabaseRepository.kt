package at.techbee.spectacled.screens.account.data

import app.cash.sqldelight.async.coroutines.awaitAsOne
import at.techbee.spectacled.db.SpectacledDatabase
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.mapper.dto.toDto
import at.techbee.spectacled.screens.icalentry.domain.IcalEntry
import io.ktor.http.Url
import kotlin.time.ExperimentalTime



suspend fun SpectacledDatabase.upsertPrincipal(principal: Principal): Long {
    val principalDto = principal.toDto()

    this.principal_dtoQueries.transaction {
        // first update, if the entry doesn't exist, this is ignored
        principal_dtoQueries.updatePrincipal(
            principalUrl = principalDto.principalUrl,
            displayName = principalDto.displayName,
            calendarUserAddressSet = principalDto.calendarUserAddressSet
        )
        // insert, but if the entry exists, it will be ignored
        principal_dtoQueries.insertPrincipal(
            principalUrl = principalDto.principalUrl,
            displayName = principalDto.displayName,
            calendarUserAddressSet = principalDto.calendarUserAddressSet
        )
    }

    return this.principal_dtoQueries.getPrincipalByUrl(principalDto.principalUrl).awaitAsOne().id
}


suspend fun SpectacledDatabase.upsertHomeCollection(homeCollection: HomeCollection, principalUrl: Url): Long {
    val homeCollectionDto = homeCollection.toDto()

    this.home_collection_dtoQueries.transaction {
        // first update, if the entry doesn't exist, this is ignored
        home_collection_dtoQueries.updateHomeCollection(
            principalUrl = principalUrl.toString(),
            url = homeCollectionDto.url,
            calDavPrivileges = homeCollectionDto.calDavPrivileges
        )
        // insert, but if the entry exists, it will be ignored
        home_collection_dtoQueries.insertHomeCollection(
            principalUrl = principalUrl.toString(),
            url = homeCollectionDto.url,
            calDavPrivileges = homeCollectionDto.calDavPrivileges
        )
    }

    return this.home_collection_dtoQueries.getHomeCollectionsByUrl(homeCollectionDto.url).awaitAsOne().id
}


suspend fun SpectacledDatabase.upsertCalendar(calendar: Calendar, homeCollectionUrl: Url): Long {
    val calendarDto = calendar.toDto()

    this.calendar_dtoQueries.transaction {

        // first update, if the entry doesn't exist, this is ignored
        calendar_dtoQueries.updateCalendar(
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
            syncComponent = calendarDto.syncComponent
        )
        // insert, but if the entry exists, it will be ignored
        calendar_dtoQueries.insertCalendar(
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
            syncComponent = calendarDto.syncComponent
        )
    }

    return this.calendar_dtoQueries.getCalendarByUrl(calendarDto.url).awaitAsOne().id
}

@OptIn(ExperimentalTime::class)
suspend fun SpectacledDatabase.insertOrUpdateIcalEntry(icalEntry: IcalEntry) {
    val icalEntryDto = icalEntry.toDto()

    icalentry_dtoQueries.transaction {
        // first update, if the UID doesn't exist, this is ignored
        icalentry_dtoQueries.updateIcalEntry(
            calendarId = icalEntryDto.calendarId,
            uid = icalEntryDto.uid,
            summary = icalEntryDto.summary,
            description = icalEntryDto.description,
            dtstart = icalEntryDto.dtstart,
            dtStartTimeZone = icalEntryDto.dtStartTimeZone,
            dtstamp = icalEntryDto.dtstamp,
            color = icalEntryDto.color,
            sequence = icalEntryDto.sequence,
            categories = icalEntryDto.categories,
            created = icalEntryDto.created,
            lastModified = icalEntryDto.lastModified,
            extraProperties = icalEntryDto.extraProperties,
            syncState = icalEntryDto.syncState,
            etag = icalEntryDto.etag,
            href = icalEntryDto.href
        )
        // insert, but if the UID exists, it will be ignored
        icalentry_dtoQueries.insertIcalEntry(
            calendarId = icalEntryDto.calendarId,
            uid = icalEntryDto.uid,
            summary = icalEntryDto.summary,
            description = icalEntryDto.description,
            dtstart = icalEntryDto.dtstart,
            dtStartTimeZone = icalEntryDto.dtStartTimeZone,
            dtstamp = icalEntryDto.dtstamp,
            color = icalEntryDto.color,
            sequence = icalEntryDto.sequence,
            categories = icalEntryDto.categories,
            created = icalEntryDto.created,
            lastModified = icalEntryDto.lastModified,
            extraProperties = icalEntryDto.extraProperties,
            syncState = icalEntryDto.syncState,
            etag = icalEntryDto.etag,
            href = icalEntryDto.href
        )
    }
}