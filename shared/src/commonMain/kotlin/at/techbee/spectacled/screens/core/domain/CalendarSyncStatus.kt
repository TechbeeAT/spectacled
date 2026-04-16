package at.techbee.spectacled.screens.core.domain

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


@Serializable
data class CalendarSyncStatus(
    val type: CalendarSyncStatusType,
    val message: String? = null,
    val details: String? = null,
    val icsDateTime: IcsDateTime = IcsDateTime.now()
) {

    companion object {

        private val mapperJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun deserialize(string: String) = mapperJson.decodeFromString<CalendarSyncStatus>(string)
    }

    fun serialize() = mapperJson.encodeToString(this)
}

@Serializable
enum class CalendarSyncStatusType {
    IN_PROGRESS,
    FAILED,
    NOT_AUTHORIZED,
    NOT_FOUND,
    SYNCED;

    fun isErrorType() = this == FAILED || this == NOT_AUTHORIZED || this == NOT_FOUND
}
