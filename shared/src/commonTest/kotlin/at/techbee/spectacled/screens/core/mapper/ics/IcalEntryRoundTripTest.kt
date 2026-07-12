package at.techbee.spectacled.screens.core.mapper.ics

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.FileManager
import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.Attachment
import at.techbee.spectacled.screens.core.domain.AttachmentSyncState
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.Classification
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import io.ktor.http.Url
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Serializes entries through the real serializer and parses them back through the real
 * parser - the exact same path an entry takes to a CalDAV server and back. ICS has
 * second precision, so all instants used here are whole seconds.
 */
class IcalEntryRoundTripTest {

    private val instant = Instant.parse("2026-07-11T10:15:30Z")
    private val utcDateTime = IcsDateTime(instant, isDateOnly = false, timeZone = TimeZone.UTC)

    private class FakeFileManager(private val content: ByteArray = ByteArray(0)) : FileManager {
        val saved = mutableMapOf<String, ByteArray>()
        override fun getAttachmentsDirectory() = "/fake"
        override fun saveAttachment(fileName: String, bytes: ByteArray): String {
            saved[fileName] = bytes
            return "/fake/$fileName"
        }
        override fun readAttachment(path: String): ByteArray = content
        override fun deleteAttachment(path: String) = false
        override fun exists(path: String) = true
    }

    @Test
    fun journalRoundTrip() {
        val original = IcalEntry(
            uid = "journal-uid-1",
            summary = "Summary with, comma; semicolon",
            description = "Multi\nline text and a Windows path C:\\notes\\file.txt",
            dtStart = IcsDateTime(LocalDate(2026, 7, 11).atStartOfDayIn(TimeZone.UTC), isDateOnly = true, timeZone = null),
            status = Status.FINAL,
            classification = Classification.PRIVATE,
            categories = listOf("Personal", "Ideas"),
            color = Color(0xFF336699),
            sequence = 3L,
            dtstamp = utcDateTime,
            created = utcDateTime,
            lastModified = utcDateTime,
            url = Url("https://example.com/entry"),
            calendarComponent = CalendarComponent.VJOURNAL
        )

        val parsed = parseIcalEntries(serializeVCalendar(original)).single()

        assertEquals(original.uid, parsed.uid)
        assertEquals(original.summary, parsed.summary)
        assertEquals(original.description, parsed.description)
        assertEquals(original.dtStart, parsed.dtStart)
        assertEquals(original.status, parsed.status)
        assertEquals(original.classification, parsed.classification)
        assertEquals(original.categories, parsed.categories)
        assertEquals(original.color, parsed.color)
        assertEquals(original.sequence, parsed.sequence)
        assertEquals(original.dtstamp, parsed.dtstamp)
        assertEquals(original.created, parsed.created)
        assertEquals(original.lastModified, parsed.lastModified)
        assertEquals(original.url, parsed.url)
        assertEquals(CalendarComponent.VJOURNAL, parsed.calendarComponent)
        assertEquals(SyncState.SYNCED, parsed.syncState)
    }

    @Test
    fun taskRoundTrip() {
        val original = IcalEntry(
            uid = "task-uid-1",
            summary = "A task",
            due = utcDateTime,
            completed = utcDateTime,
            status = Status.IN_PROCESS,
            percentComplete = 50L,
            priority = 1L,
            dtstamp = utcDateTime,
            created = utcDateTime,
            lastModified = utcDateTime,
            calendarComponent = CalendarComponent.VTODO
        )

        val parsed = parseIcalEntries(serializeVCalendar(original)).single()

        assertEquals(CalendarComponent.VTODO, parsed.calendarComponent)
        assertEquals(original.due, parsed.due)
        assertEquals(original.completed, parsed.completed)
        assertEquals(Status.IN_PROCESS, parsed.status)
        assertEquals(50L, parsed.percentComplete)
        assertEquals(1L, parsed.priority)
    }

    @Test
    fun multipleEntriesRoundTripInOneVCalendar() {
        val journal = IcalEntry(uid = "multi-journal", dtstamp = utcDateTime, created = utcDateTime, calendarComponent = CalendarComponent.VJOURNAL)
        val task = IcalEntry(uid = "multi-task", dtstamp = utcDateTime, created = utcDateTime, calendarComponent = CalendarComponent.VTODO)

        val parsed = parseIcalEntries(serializeVCalendar(listOf(journal, task)))

        assertEquals(2, parsed.size)
        assertEquals("multi-journal", parsed[0].uid)
        assertEquals(CalendarComponent.VJOURNAL, parsed[0].calendarComponent)
        assertEquals("multi-task", parsed[1].uid)
        assertEquals(CalendarComponent.VTODO, parsed[1].calendarComponent)
    }

    @Test
    fun longDescriptionSurvivesLineFolding() {
        val longText = "All work and no play makes Jack a dull boy. ".repeat(8).trim()
        val original = IcalEntry(uid = "fold-uid", description = longText, dtstamp = utcDateTime, created = utcDateTime, calendarComponent = CalendarComponent.VJOURNAL)

        val ics = serializeVCalendar(original)
        assertTrue(ics.contains("\r\n "), "Expected the long description to be folded")
        assertEquals(longText, parseIcalEntries(ics).single().description)
    }

    @Test
    fun zonedDtStartRoundTripsAndEmitsVTimezone() {
        val vienna = TimeZone.of("Europe/Vienna")
        val original = IcalEntry(
            uid = "tz-uid",
            dtStart = IcsDateTime(instant, isDateOnly = false, timeZone = vienna),
            dtstamp = utcDateTime,
            created = utcDateTime,
            calendarComponent = CalendarComponent.VJOURNAL
        )

        val ics = serializeVCalendar(original)
        assertTrue(ics.contains("BEGIN:VTIMEZONE"))
        assertTrue(ics.contains("TZID:Europe/Vienna"))
        // Vienna observes DST in 2026, so both observance blocks must be present
        assertTrue(ics.contains("BEGIN:DAYLIGHT"))
        assertTrue(ics.contains("BEGIN:STANDARD"))
        assertTrue(ics.contains("DTSTART;TZID=Europe/Vienna:"))

        assertEquals(original.dtStart, parseIcalEntries(ics).single().dtStart)
    }

    @Test
    fun zoneWithoutDstEmitsOnlyStandardBlock() {
        val original = IcalEntry(
            uid = "tokyo-uid",
            dtStart = IcsDateTime(instant, isDateOnly = false, timeZone = TimeZone.of("Asia/Tokyo")),
            dtstamp = utcDateTime,
            created = utcDateTime,
            calendarComponent = CalendarComponent.VJOURNAL
        )

        val ics = serializeVCalendar(original)
        assertTrue(ics.contains("TZID:Asia/Tokyo"))
        assertTrue(ics.contains("BEGIN:STANDARD"))
        assertFalse(ics.contains("BEGIN:DAYLIGHT"))
    }

    @Test
    fun utcOnlyEntriesEmitNoVTimezone() {
        val original = IcalEntry(uid = "utc-uid", dtStart = utcDateTime, dtstamp = utcDateTime, created = utcDateTime, calendarComponent = CalendarComponent.VJOURNAL)
        assertFalse(serializeVCalendar(original).contains("VTIMEZONE"))
    }

    @Test
    fun relatedToWithExplicitRelTypeRoundTrips() {
        val original = IcalEntry(uid = "child-uid", parentUid = "parent-uid", relType = "CHILD", dtstamp = utcDateTime, created = utcDateTime, calendarComponent = CalendarComponent.VTODO)
        val parsed = parseIcalEntries(serializeVCalendar(original)).single()
        assertEquals("parent-uid", parsed.parentUid)
        assertEquals("CHILD", parsed.relType)
    }

    @Test
    fun relatedToWithoutRelTypeDefaultsToParent() {
        // RFC 5545: RELTYPE defaults to PARENT when the parameter is absent, so the
        // parser materializes the default rather than keeping null.
        val original = IcalEntry(uid = "child-uid", parentUid = "parent-uid", relType = null, dtstamp = utcDateTime, created = utcDateTime, calendarComponent = CalendarComponent.VTODO)
        val parsed = parseIcalEntries(serializeVCalendar(original)).single()
        assertEquals("parent-uid", parsed.parentUid)
        assertEquals("PARENT", parsed.relType)
    }

    @Test
    fun unknownPropertiesRoundTripAsExtraProperties() {
        val raw = RawIcsProperty(name = "X-CUSTOM", unfoldedLine = "X-CUSTOM;X-PARAM=1:some value")
        val original = IcalEntry(uid = "extra-uid", extraProperties = listOf(raw), dtstamp = utcDateTime, created = utcDateTime, calendarComponent = CalendarComponent.VJOURNAL)
        assertEquals(listOf(raw), parseIcalEntries(serializeVCalendar(original)).single().extraProperties)
    }

    @Test
    fun uriAttachmentRoundTrips() {
        val original = IcalEntry(
            uid = "att-uid",
            attachments = listOf(
                Attachment(
                    remoteUrl = "https://example.com/files/doc.pdf",
                    fileName = "doc.pdf",
                    mimeType = "application/pdf",
                    isInline = false,
                    syncState = AttachmentSyncState.SYNCED
                )
            ),
            dtstamp = utcDateTime,
            created = utcDateTime,
            calendarComponent = CalendarComponent.VJOURNAL
        )

        val attachment = parseIcalEntries(serializeVCalendar(original)).single().attachments.single()
        assertEquals("https://example.com/files/doc.pdf", attachment.remoteUrl)
        assertEquals("doc.pdf", attachment.fileName)
        assertEquals("application/pdf", attachment.mimeType)
        assertFalse(attachment.isInline)
        assertEquals(AttachmentSyncState.PENDING_DOWNLOAD, attachment.syncState)
    }

    @Test
    fun inlineBinaryAttachmentRoundTripsThroughBase64() {
        val bytes = "hello world".encodeToByteArray()
        val writerFileManager = FakeFileManager(bytes)
        val original = IcalEntry(
            uid = "bin-uid",
            attachments = listOf(
                Attachment(localPath = "/fake/src.txt", fileName = "hello.txt", mimeType = "text/plain", isInline = true)
            ),
            dtstamp = utcDateTime,
            created = utcDateTime,
            calendarComponent = CalendarComponent.VJOURNAL
        )

        val ics = serializeVCalendar(original, writerFileManager)
        assertTrue(ics.contains("VALUE=BINARY"))
        assertTrue(ics.contains("ENCODING=BASE64"))

        val readerFileManager = FakeFileManager()
        val attachment = parseIcalEntries(ics, readerFileManager).single().attachments.single()
        assertTrue(attachment.isInline)
        assertEquals(AttachmentSyncState.SYNCED, attachment.syncState)
        assertEquals("hello.txt", attachment.fileName)
        assertEquals("text/plain", attachment.mimeType)
        assertEquals(bytes.size.toLong(), attachment.size)
        assertContentEquals(bytes, readerFileManager.saved.values.single())
        assertEquals("/fake/${readerFileManager.saved.keys.single()}", attachment.localPath)
    }

    @Test
    fun veventComponentsAreIgnored() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VEVENT",
            "UID:event-1",
            "END:VEVENT",
            "BEGIN:VJOURNAL",
            "UID:journal-1",
            "END:VJOURNAL",
            "END:VCALENDAR"
        ).joinToString("\r\n")

        val parsed = parseIcalEntries(ics)
        assertEquals(1, parsed.size)
        assertEquals("journal-1", parsed.single().uid)
    }

    @Test
    fun blockWithoutUidIsSkippedInsteadOfAborting() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VJOURNAL",
            "SUMMARY:No UID here",
            "END:VJOURNAL",
            "BEGIN:VJOURNAL",
            "UID:valid-uid",
            "END:VJOURNAL",
            "END:VCALENDAR"
        ).joinToString("\r\n")

        val parsed = parseIcalEntries(ics)
        assertEquals(1, parsed.size)
        assertEquals("valid-uid", parsed.single().uid)
    }

    @Test
    fun parsesServerStyleFoldedInput() {
        val ics = listOf(
            "BEGIN:VCALENDAR",
            "BEGIN:VJOURNAL",
            "UID:folded-uid",
            "SUMMARY:This summary is spread over mult",
            " iple folded lines",
            "END:VJOURNAL",
            "END:VCALENDAR"
        ).joinToString("\r\n")

        assertEquals("This summary is spread over multiple folded lines", parseIcalEntries(ics).single().summary)
    }
}
