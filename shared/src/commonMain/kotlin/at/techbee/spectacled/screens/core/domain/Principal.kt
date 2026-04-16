package at.techbee.spectacled.screens.core.domain

import io.ktor.http.Url

data class Principal(
    val id: Long,
    val principalUrl: Url,
    var displayName: String?,
    var calendarUserAddressSet: List<String>
) {

    companion object {
        fun getPrincipalForPreview(): Principal = Principal(
            id = 0,
            principalUrl = Url("https://example.com"),
            displayName = "Principal",
            calendarUserAddressSet = emptyList()
        )
    }
}