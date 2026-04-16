package at.techbee.spectacled.screens.core.data.webdav

import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.note.domain.Note
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url


sealed class DiscoverPrincipalsResult {
    data class Success(val principals: List<Principal>) : DiscoverPrincipalsResult()
    data object NotFound : DiscoverPrincipalsResult()
    data object NotAuthorized : DiscoverPrincipalsResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : DiscoverPrincipalsResult()
}

sealed class DiscoverHomeCollectionsResult {
    data class Success(val principalDisplayName: String?, val principalCalendarUserAddressSet: List<String>, val homeCollections: List<HomeCollection>) : DiscoverHomeCollectionsResult()
    data object NotFound : DiscoverHomeCollectionsResult()
    data object NotAuthorized : DiscoverHomeCollectionsResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : DiscoverHomeCollectionsResult()
}

sealed class DiscoverCalendarsResult {
    data class Success(val calDavPrivileges: List<CalDavPrivilege>, val calendars: List<Calendar>) : DiscoverCalendarsResult()
    data object NotFound : DiscoverCalendarsResult()
    data object NotAuthorized : DiscoverCalendarsResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : DiscoverCalendarsResult()
}

sealed class MultigetResourceHrefETagResult {
    data class Success(val hrefs: Map<Url, String?>, val syncToken: String?) : MultigetResourceHrefETagResult()
    data object NotFound : MultigetResourceHrefETagResult()
    data object NotAuthorized : MultigetResourceHrefETagResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : MultigetResourceHrefETagResult()
}

sealed class MultigetResourceResult {
    data class Success(val notes: List<Note>) : MultigetResourceResult()
    data object NotFound : MultigetResourceResult()
    data object NotAuthorized : MultigetResourceResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : MultigetResourceResult()
}

sealed class MultigetSyncCollectionResult {
    data class Success(val syncToken: String?, val hrefs: Map<Url, String?>) : MultigetSyncCollectionResult()
    data object NotFound : MultigetSyncCollectionResult()
    data object NotAuthorized : MultigetSyncCollectionResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : MultigetSyncCollectionResult()
}

sealed class UpsertCalendarResult {
    data class Success(val calendar: Calendar) : UpsertCalendarResult()
    data object NotFound : UpsertCalendarResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : UpsertCalendarResult()
}


sealed class DeleteCalendarResult {
    data object SuccessfullyDeleted : DeleteCalendarResult()
    data object AlreadyDeleted : DeleteCalendarResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : DeleteCalendarResult()
}


sealed class DeleteResourceResult {
    data object Success : DeleteResourceResult()
    data object AlreadyDeleted : DeleteResourceResult()
    data object Conflict : DeleteResourceResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : DeleteResourceResult()
}

sealed class GetResourceResult {
    data class Success(val note: Note) : GetResourceResult()
    data object NotFound : GetResourceResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : GetResourceResult()
}

sealed class PutResourceResult {
    data class Success(val note: Note) : PutResourceResult()
    data object Conflict : PutResourceResult()
    data object NotFound : PutResourceResult()
    data class Failed(val status: HttpStatusCode, val message: String, val details: String? = null) : PutResourceResult()
}
