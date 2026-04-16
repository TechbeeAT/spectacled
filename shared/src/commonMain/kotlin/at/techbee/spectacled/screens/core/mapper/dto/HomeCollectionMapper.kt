package at.techbee.spectacled.screens.core.mapper.dto

import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.sqldelight.HomeCollectionDto
import io.ktor.http.URLParserException
import io.ktor.http.Url

fun HomeCollectionDto.toDomain(): HomeCollection {
    return HomeCollection(
        id = this.id,
        principalId = this.principalId,
        url = try { Url(this.url) } catch (_: URLParserException) { Url("") },
        calDavPrivileges = this.calDavPrivileges?.split(',')?.mapNotNull { CalDavPrivilege.fromTag(it) }?: emptyList()
    )
}

fun HomeCollection.toDto(): HomeCollectionDto {
    return HomeCollectionDto(
        id = this.id,
        principalId = this.principalId,
        url = this.url.toString(),
        calDavPrivileges = this.calDavPrivileges.joinToString(",") { it.tag }
    )
}