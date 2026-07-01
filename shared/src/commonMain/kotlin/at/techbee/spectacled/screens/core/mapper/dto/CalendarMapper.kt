package at.techbee.spectacled.screens.core.mapper.dto

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.sqldelight.CalendarDto
import io.ktor.http.URLParserException
import io.ktor.http.Url


fun CalendarDto.toDomain(): Calendar {
    return Calendar(
        id = this.id,
        homeCollectionId = this.homeCollectionId,
        url = try {
            Url(this.url)
        } catch (_: URLParserException) {
            Url("")
        },
        displayName = this.displayName,
        calendarDescription = this.calendarDescription,
        color = this.color?.let { Color(it) },
        ctag = this.ctag,
        supportedComponents = this.supportedComponents?.split(',')
            ?.mapNotNull { componentName ->
                try {
                    CalendarComponent.valueOf(componentName.trim())
                } catch (_: IllegalArgumentException) {
                    null // Ignore unknown components
                }
            } ?: emptyList(),
        calDavPrivileges = this.calDavPrivileges?.split(',')?.mapNotNull { CalDavPrivilege.fromTag(it) }?: emptyList(),
        calendarSyncStatus = this.calendarSyncStatus?.let { CalendarSyncStatus.deserialize(it) },
        syncToken = this.syncToken,
        syncComponent = this.syncComponent?.let { CalendarComponent.entries.find { entry -> entry.name == it }},
        attachmentCollectionUrl = this.attachmentCollectionUrl?.let { Url(it) }
    )
}

fun Calendar.toDto(): CalendarDto {
    return CalendarDto(
        id = this.id,
        homeCollectionId = this.homeCollectionId,
        url = this.url.toString(),
        displayName = this.displayName,
        calendarDescription = this.calendarDescription,
        color = this.color?.toArgb()?.toLong(),
        ctag = this.ctag,
        supportedComponents = this.supportedComponents.joinToString(","),
        calDavPrivileges = this.calDavPrivileges.joinToString(",") { it.tag },
        calendarSyncStatus = this.calendarSyncStatus?.serialize(),
        syncToken = this.syncToken,
        syncComponent = this.syncComponent?.name,
        attachmentCollectionUrl = this.attachmentCollectionUrl?.toString()
    )
}
