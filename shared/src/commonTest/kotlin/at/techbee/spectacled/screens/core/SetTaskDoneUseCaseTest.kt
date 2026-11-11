package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.alarm.AlarmCoordinator
import at.techbee.spectacled.screens.core.alarm.FakeAlarmScheduler
import at.techbee.spectacled.screens.core.alarm.FakeIcalEntryRepository
import at.techbee.spectacled.screens.core.alarm.ScheduledAlarmKey
import at.techbee.spectacled.screens.core.data.ics.IcsDuration
import at.techbee.spectacled.screens.core.data.ics.RawIcsProperty
import at.techbee.spectacled.screens.core.domain.Alarm
import at.techbee.spectacled.screens.core.domain.AlarmTrigger
import at.techbee.spectacled.screens.core.domain.CalDavPrivilege
import at.techbee.spectacled.screens.core.domain.Calendar
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.core.domain.HomeCollection
import at.techbee.spectacled.screens.core.domain.IcalEntry
import at.techbee.spectacled.screens.core.domain.Principal
import at.techbee.spectacled.screens.core.domain.Status
import at.techbee.spectacled.screens.core.domain.SyncState
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SetTaskDoneUseCaseTest {

    private val writable = Calendar.getCalendarForPreview().copy(id = 1L, calDavPrivileges = listOf(CalDavPrivilege.WRITE_CONTENT))
    private val readOnly = Calendar.getCalendarForPreview().copy(id = 2L, calDavPrivileges = listOf(CalDavPrivilege.READ))

    private val alarm = Alarm(uid = "alarm-1", trigger = AlarmTrigger.Relative(IcsDuration()))
    private fun task(id: Long, calendarId: Long = writable.id) =
        IcalEntry.newTask().copy(id = id, calendarId = calendarId, syncState = SyncState.SYNCED, alarms = listOf(alarm))

    private class Fixture(entries: List<IcalEntry>, calendars: List<Calendar>) {
        val repository = FakeIcalEntryRepository(entries = entries)
        val scheduler = FakeAlarmScheduler()
        val syncTrigger = RecordingSyncTrigger()
        val useCase = SetTaskDoneUseCase(
            repository,
            CalendarsByIdRepository(calendars),
            AlarmCoordinator(repository, scheduler, dispatcher = Dispatchers.Unconfined),
            syncTrigger
        )
    }

    @Test
    fun done_updatesTheTaskDismissesItsNotificationsAndSyncs() = runTest {
        val fixture = Fixture(listOf(task(5L)), listOf(writable))

        assertTrue(fixture.useCase(5L, done = true))

        val update = fixture.repository.progressUpdates.single()
        assertEquals(5L, update.id)
        assertEquals(100L, update.percentComplete)
        assertEquals(Status.COMPLETED, update.status)
        assertEquals(SyncState.LOCAL_MODIFIED, update.syncState)
        assertEquals(listOf(ScheduledAlarmKey(5L, "alarm-1")), fixture.scheduler.dismissed)
        assertEquals(listOf(listOf(writable.id)), fixture.syncTrigger.requested)
    }

    @Test
    fun notDone_reopensTheTaskWithoutDismissing() = runTest {
        val fixture = Fixture(listOf(task(5L).copy(percentComplete = 100L, status = Status.COMPLETED)), listOf(writable))

        assertTrue(fixture.useCase(5L, done = false))

        assertEquals(0L, fixture.repository.progressUpdates.single().percentComplete)
        assertTrue(fixture.scheduler.dismissed.isEmpty())
        assertEquals(1, fixture.scheduler.replaceAllCalls.size, "reconciled, the alarms may be due again")
    }

    @Test
    fun entriesThatCantBeChangedAreLeftAlone() = runTest {
        val entries = listOf(
            task(1L, calendarId = readOnly.id),
            task(2L).copy(syncState = SyncState.LOCAL_DELETED),
            task(3L).copy(extraProperties = listOf(RawIcsProperty("RRULE", "RRULE:FREQ=DAILY"))),
            task(4L).copy(calendarComponent = CalendarComponent.VJOURNAL),
            task(6L, calendarId = 99L)    // calendar is gone
        )
        val fixture = Fixture(entries, listOf(writable, readOnly))

        (entries.map { it.id } + 404L).forEach { id ->
            assertFalse(fixture.useCase(id, done = true), "entry $id")
        }
        assertTrue(fixture.repository.progressUpdates.isEmpty())
        assertTrue(fixture.scheduler.dismissed.isEmpty())
        assertTrue(fixture.syncTrigger.requested.isEmpty())
    }
}

private class RecordingSyncTrigger : SyncTrigger {
    val requested = mutableListOf<List<Long>>()
    override fun requestImmediate(calendarIds: List<Long>) { requested += calendarIds }
    override fun requestImmediate() = TODO()
    override fun schedulePeriodic() = TODO()
    override fun cancel() = TODO()
    override fun triggerWidgetUpdate() = TODO()
}

private class CalendarsByIdRepository(private val calendars: List<Calendar>) : CalendarRepository {
    override suspend fun getCalendarById(id: Long): Calendar? = calendars.find { it.id == id }

    // --- not exercised ---
    override fun getAllPrincipalsFlow(): Flow<List<Principal>> = TODO()
    override fun getAllHomeCollectionsFlow(): Flow<List<HomeCollection>> = TODO()
    override fun getAllCalendarsFlow(): Flow<List<Calendar>> = TODO()
    override suspend fun getAllPrincipals(): List<Principal> = TODO()
    override suspend fun getAllHomeCollections(): List<HomeCollection> = TODO()
    override suspend fun getAllCalendars(): List<Calendar> = TODO()
    override suspend fun getPrincipalForCalendar(calendarId: Long): Principal? = TODO()
    override suspend fun getPrincipalUrlForCalendarId(calendarId: Long): String? = TODO()
    override suspend fun getCalendarsForPrincipalUrl(principalUrl: String): List<Calendar> = TODO()
    override suspend fun getCalendarsByIds(calendarIds: List<Long>): List<Calendar> = TODO()
    override suspend fun upsertPrincipal(principal: Principal): Long = TODO()
    override suspend fun upsertHomeCollection(homeCollection: HomeCollection, principalUrl: Url): Long = TODO()
    override suspend fun upsertCalendar(calendar: Calendar, homeCollectionUrl: Url): Long = TODO()
    override suspend fun deletePrincipal(id: Long) = TODO()
    override suspend fun deleteCalendar(id: Long) = TODO()
    override suspend fun updateCalendarSyncStatus(calendarSyncStatus: String?, syncToken: String?, id: Long) = TODO()
}
