package at.techbee.spectacled.screens.core.domain

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarTest {

    private val base = Calendar.getCalendarForPreview()

    @Test
    fun canWriteContent_requiresAWritePrivilege() {
        assertFalse(base.copy(calDavPrivileges = emptyList()).canWriteContent())
        assertFalse(base.copy(calDavPrivileges = listOf(CalDavPrivilege.READ)).canWriteContent())
        assertTrue(base.copy(calDavPrivileges = listOf(CalDavPrivilege.WRITE_CONTENT)).canWriteContent())
        assertTrue(base.copy(calDavPrivileges = listOf(CalDavPrivilege.WRITE)).canWriteContent())
        assertTrue(base.copy(calDavPrivileges = listOf(CalDavPrivilege.ALL)).canWriteContent())
        // write-properties alone does not grant content access
        assertFalse(base.copy(calDavPrivileges = listOf(CalDavPrivilege.WRITE_PROPERTIES)).canWriteContent())
    }

    @Test
    fun canWriteProperties_requiresAPropertiesPrivilege() {
        assertFalse(base.copy(calDavPrivileges = emptyList()).canWriteProperties())
        assertFalse(base.copy(calDavPrivileges = listOf(CalDavPrivilege.READ)).canWriteProperties())
        assertTrue(base.copy(calDavPrivileges = listOf(CalDavPrivilege.WRITE_PROPERTIES)).canWriteProperties())
        assertTrue(base.copy(calDavPrivileges = listOf(CalDavPrivilege.WRITE)).canWriteProperties())
        assertTrue(base.copy(calDavPrivileges = listOf(CalDavPrivilege.ALL)).canWriteProperties())
        // write-content alone does not grant property access
        assertFalse(base.copy(calDavPrivileges = listOf(CalDavPrivilege.WRITE_CONTENT)).canWriteProperties())
    }

    @Test
    fun tasksSupportedRequiresVtodo() {
        assertTrue(base.copy(supportedComponents = listOf(CalendarComponent.VTODO)).isTasksSupported())
        assertTrue(base.copy(supportedComponents = listOf(CalendarComponent.VJOURNAL, CalendarComponent.VTODO)).isTasksSupported())
        assertFalse(base.copy(supportedComponents = listOf(CalendarComponent.VJOURNAL)).isTasksSupported())
        assertFalse(base.copy(supportedComponents = emptyList()).isTasksSupported())
    }

    @Test
    fun attachmentSyncSupportedRequiresCollectionUrl() {
        assertFalse(base.copy(attachmentCollectionUrl = null).isManagedAttachmentSyncSupported())
        assertTrue(base.copy(attachmentCollectionUrl = Url("https://example.com/attachments/")).isManagedAttachmentSyncSupported())
    }

    @Test
    fun privilegeTagLookupIsCaseInsensitive() {
        assertEquals(CalDavPrivilege.WRITE_CONTENT, CalDavPrivilege.fromTag("write-content"))
        assertEquals(CalDavPrivilege.WRITE_CONTENT, CalDavPrivilege.fromTag("WRITE-CONTENT"))
        assertNull(CalDavPrivilege.fromTag("no-such-privilege"))
    }
}
