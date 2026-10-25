package at.techbee.spectacled.screenshots

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.core.data.Credentials
import at.techbee.spectacled.screens.core.data.PlatformCredentialStore
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatus
import at.techbee.spectacled.screens.core.domain.CalendarSyncStatusType
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.sql.DriverManager
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class SeededSampleData(
    val calendarId: Long,
    val icalEntryIdsByUid: Map<String, Long>
)

/**
 * Fills the (isolated) app database with two fake accounts and their calendars, and the first
 * calendar with the entries of sample-data/screenshot_entries.sql that belong to the app being rendered.
 */
internal object SampleData {

    /** The "today" screenshot_entries.sql was written for: its dates are shifted by today minus this. */
    private val SAMPLE_DATA_TODAY = LocalDate.of(2026, 7, 16)

    // Never contacted: the generator's HttpClient is offline (see Main.kt), and .example is reserved (RFC 2606).
    private const val PRIVATE_SERVER = "https://dav.example.com"
    private const val WORK_SERVER = "https://cloud.example.org"

    /** A calendar shown in the account list; the first one of the private account holds the sample entries. */
    private class SampleCalendar(val name: String, val color: Color)

    /** Two accounts with a few calendars each, so the account list looks lived in. */
    private fun privateCalendars(variant: SpectacledVariant) = when (variant) {
        SpectacledVariant.JOURNALS -> listOf(SampleCalendar("Journal", variant.themeSeedColor), SampleCalendar("Travel log", Color(0xFF2E7D32)), SampleCalendar("Dream diary", Color(0xFF6A1B9A)))
        SpectacledVariant.NOTES -> listOf(SampleCalendar("Notes", variant.themeSeedColor), SampleCalendar("Recipes", Color(0xFFEF6C00)), SampleCalendar("Ideas", Color(0xFF00838F)))
        SpectacledVariant.TASKS -> listOf(SampleCalendar("Tasks", variant.themeSeedColor), SampleCalendar("Household", Color(0xFF1565C0)), SampleCalendar("Shopping", Color(0xFFC62828)))
    }

    private fun workCalendars(variant: SpectacledVariant) = when (variant) {
        SpectacledVariant.JOURNALS -> listOf(SampleCalendar("Meeting minutes", Color(0xFF37474F)), SampleCalendar("Project log", Color(0xFFAD1457)))
        SpectacledVariant.NOTES -> listOf(SampleCalendar("Team wiki", Color(0xFF37474F)), SampleCalendar("Meetings", Color(0xFFAD1457)))
        SpectacledVariant.TASKS -> listOf(SampleCalendar("Sprint", Color(0xFF37474F)), SampleCalendar("Projects", Color(0xFFAD1457)))
    }

    private val quotedIcsDate = Regex("'(\\d{4})(\\d{2})(\\d{2})(T\\d{6}Z?)?'")

    private val SYNCED get() = CalendarSyncStatus(CalendarSyncStatusType.SYNCED)

    /**
     * Shows all calendars as synced again. Leaving a details screen requests a sync (OnDispose),
     * which fails against the offline HttpClient of the generator; called before every scene, this
     * lets such a sync from the previous scene finish and then undoes its "sync problem".
     */
    suspend fun resetSyncStatus(calendarRepository: CalendarRepository) {
        delay(300.milliseconds)   // a sync requested while the previous scene closed gets to start
        withTimeoutOrNull(10.seconds) {
            while (calendarRepository.getAllCalendars().any { it.calendarSyncStatus?.type == CalendarSyncStatusType.IN_PROGRESS })
                delay(20.milliseconds)
        }
        calendarRepository.getAllCalendars().forEach { calendar ->
            calendarRepository.updateCalendarSyncStatus(SYNCED.serialize(), syncToken = null, id = calendar.id)
        }
    }

    suspend fun seed(
        variant: SpectacledVariant,
        sqlFile: File,
        calendarRepository: CalendarRepository,
        credentialStore: PlatformCredentialStore
    ): SeededSampleData {

        // Accounts and calendars go through the app's own repository, so they are stored exactly
        // like synced ones. This also creates the database file the entries are inserted into.
        val calendarId = addAccount(calendarRepository, credentialStore, variant, PRIVATE_SERVER, "alex", "alex@example.com", privateCalendars(variant)).first()
        addAccount(calendarRepository, credentialStore, variant, WORK_SERVER, "alex.miller", "Alex Miller (Work)", workCalendars(variant))

        // Same path DatabaseDriverFactory.jvm uses; user.home points to the isolated directory.
        val dbFile = File(System.getProperty("user.home"), ".spectacled/${variant.dbName}")
        check(dbFile.exists()) { "Expected the app database at $dbFile" }

        val uidPrefix = when (variant) {
            SpectacledVariant.JOURNALS -> "'sample-journal-"
            SpectacledVariant.NOTES -> "'sample-note-"
            SpectacledVariant.TASKS -> "'sample-task-"
        }
        val dayShift = ChronoUnit.DAYS.between(SAMPLE_DATA_TODAY, LocalDate.now())

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                splitStatements(sqlFile.readText())
                    .filter { it.contains(uidPrefix) }
                    .forEach { sql ->
                        statement.executeUpdate(
                            shiftDates(sql.replace("<calendarId>", calendarId.toString()), dayShift)
                        )
                    }
            }

            val icalEntryIdsByUid = connection.prepareStatement("SELECT uid, id FROM IcalEntryDto WHERE calendarId = ?").use { query ->
                query.setLong(1, calendarId)
                query.executeQuery().use { rows ->
                    buildMap {
                        while (rows.next())
                            put(rows.getString(1), rows.getLong(2))
                    }
                }
            }
            check(icalEntryIdsByUid.isNotEmpty()) { "No sample entries starting with $uidPrefix in $sqlFile" }

            return SeededSampleData(calendarId, icalEntryIdsByUid)
        }
    }

    /** Stores an account with [calendars] and returns their ids in the same order. */
    private suspend fun addAccount(
        calendarRepository: CalendarRepository,
        credentialStore: PlatformCredentialStore,
        variant: SpectacledVariant,
        server: String,
        username: String,
        displayName: String,
        calendars: List<SampleCalendar>
    ): List<Long> {
        val principalUrl = Url("$server/principals/$username/")
        val homeCollectionUrl = Url("$server/calendars/$username/")

        calendarRepository.upsertPrincipal(Principal(id = 0L, principalUrl = principalUrl, displayName = displayName, calendarUserAddressSet = emptyList()))
        credentialStore.save(Credentials(server = principalUrl, username = username, password = "screenshots"))
        calendarRepository.upsertHomeCollection(
            HomeCollection(id = 0L, principalId = 0L, url = homeCollectionUrl, calDavPrivileges = listOf(CalDavPrivilege.ALL)),
            principalUrl
        )
        return calendars.map { calendar ->
            calendarRepository.upsertCalendar(
                Calendar(
                    id = 0L,
                    homeCollectionId = 0L,
                    url = Url("$homeCollectionUrl${calendar.name.lowercase().replace(' ', '-')}/"),
                    displayName = calendar.name,
                    calendarDescription = null,
                    color = calendar.color,
                    ctag = null,
                    supportedComponents = listOf(variant.mainCalendarComponent),
                    calDavPrivileges = listOf(CalDavPrivilege.ALL),
                    calendarSyncStatus = SYNCED,
                    syncToken = null
                ),
                homeCollectionUrl
            )
        }
    }

    /** Moves every quoted YYYYMMDD[THHMMSS] by [days], so "tomorrow" in the sample data stays tomorrow. */
    private fun shiftDates(sql: String, days: Long): String = sql.replace(quotedIcsDate) { match ->
        val (year, month, day, time) = match.destructured
        val shifted = LocalDate.of(year.toInt(), month.toInt(), day.toInt()).plusDays(days)
        "'%04d%02d%02d%s'".format(shifted.year, shifted.monthValue, shifted.dayOfMonth, time)
    }

    /** Splits a SQL script at the semicolons outside string literals, dropping `--` comments. */
    private fun splitStatements(script: String): List<String> {
        val statements = mutableListOf<String>()
        val current = StringBuilder()
        var inString = false
        var i = 0
        while (i < script.length) {
            val c = script[i]
            when {
                !inString && c == '-' && script.getOrNull(i + 1) == '-' -> {
                    while (i < script.length && script[i] != '\n') i++
                    continue
                }
                !inString && c == ';' -> {
                    current.toString().trim().takeIf { it.isNotEmpty() }?.let { statements += it }
                    current.clear()
                }
                else -> {
                    if (c == '\'') inString = !inString    // '' inside a literal toggles twice
                    current.append(c)
                }
            }
            i++
        }
        current.toString().trim().takeIf { it.isNotEmpty() }?.let { statements += it }
        return statements
    }
}
