package at.techbee.spectacled.screens.core.mapper.dto

import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.sqldelight.PrincipalDto
import io.ktor.http.URLParserException
import io.ktor.http.Url

fun PrincipalDto.toDomain(): Principal {

    return Principal(
        id = this.id,
        principalUrl = try { Url(this.principalUrl) } catch (_: URLParserException) { Url("") },
        displayName = this.displayName,
        calendarUserAddressSet = this.calendarUserAddressSet?.split(',') ?: emptyList()
    )
}

fun Principal.toDto(): PrincipalDto {
    return PrincipalDto(
        id = this.id,
        principalUrl = this.principalUrl.toString(),
        displayName = this.displayName,
        calendarUserAddressSet = this.calendarUserAddressSet.joinToString(",").ifEmpty { null }
    )
}
