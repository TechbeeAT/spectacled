package at.techbee.spectacled.screens.core

interface SyncTrigger {
    fun requestImmediate()
    fun requestImmediate(calendarIds: List<Long>)
    fun requestImmediatePush(calendarId: Long)
    fun schedulePeriodic()
    fun cancel()
}

expect class PlatformSyncTrigger: SyncTrigger {
    override fun requestImmediate()
    override fun requestImmediate(calendarIds: List<Long>)
    override fun requestImmediatePush(calendarId: Long)  // TODO: REVIEW! Currently not in use!
    override fun schedulePeriodic()
    override fun cancel()
}