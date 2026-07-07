package at.techbee.spectacled.screens.core.domain

import androidx.compose.ui.graphics.Color
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Calendar(
    val id: Long,
    val homeCollectionId: Long,
    val url: Url,
    val displayName: String?,
    val calendarDescription: String?,
    val color: Color?,
    val ctag: String?,
    val supportedComponents: List<CalendarComponent>, // A type-safe List
    val calDavPrivileges: List<CalDavPrivilege>, // A type-safe List
    val calendarSyncStatus: CalendarSyncStatus?,
    val syncToken: String?,
    val attachmentCollectionUrl: Url? = null
) {
    fun isAttachmentSyncSupported(): Boolean = attachmentCollectionUrl != null

    companion object {
        fun getCalendarForPreview(): Calendar = Calendar(
            id = 0,
            homeCollectionId = 0,
            url = Url("https://example.com"),
            displayName = "Calendar",
            calendarDescription = "Description",
            color = null,
            ctag = "",
            supportedComponents = emptyList(),
            calDavPrivileges = emptyList(),
            calendarSyncStatus = null,
            syncToken = null
        )

        @OptIn(ExperimentalUuidApi::class)
        fun getNewCalendar(homeCollection: HomeCollection): Calendar {
            return Calendar(
                homeCollectionId = homeCollection.id,   // TODO: taking first home collection, check if better solution can be found!
                id = 0L,
                url = URLBuilder(homeCollection.url).appendPathSegments(Uuid.random().toString()).build(),
                displayName = "",
                calendarDescription = null,
                color = null,
                ctag = null,
                supportedComponents = emptyList(),
                calDavPrivileges = emptyList(),
                calendarSyncStatus = null,
                syncToken = null
            )
        }
    }

    fun canWriteContent(): Boolean {
        return calDavPrivileges.contains(CalDavPrivilege.ALL)
                || calDavPrivileges.contains(CalDavPrivilege.WRITE)
                || calDavPrivileges.contains(CalDavPrivilege.WRITE_CONTENT)
    }

    fun canWriteProperties(): Boolean {
        return calDavPrivileges.contains(CalDavPrivilege.ALL)
                || calDavPrivileges.contains(CalDavPrivilege.WRITE)
                || calDavPrivileges.contains(CalDavPrivilege.WRITE_PROPERTIES)
    }
}