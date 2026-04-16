package at.techbee.spectacled.screens.core.domain

import io.ktor.http.Url

data class HomeCollection(
    val id: Long,
    val principalId: Long,
    val url: Url,
    var calDavPrivileges: List<CalDavPrivilege>
) {
    companion object {
        fun getHomeCollectionForPreview() = HomeCollection(
            id = 0,
            principalId = 0,
            calDavPrivileges = listOf(CalDavPrivilege.UNBIND),
            url = Url("https://localhost/home-collection")
        )
    }

    fun canBind(): Boolean {
        return calDavPrivileges.contains(CalDavPrivilege.ALL)
                || calDavPrivileges.contains(CalDavPrivilege.WRITE)
                || calDavPrivileges.contains(CalDavPrivilege.BIND)
    }

    fun canUnbind(): Boolean {
        return calDavPrivileges.contains(CalDavPrivilege.ALL)
                || calDavPrivileges.contains(CalDavPrivilege.WRITE)
                || calDavPrivileges.contains(CalDavPrivilege.UNBIND)
    }
}