package at.techbee.spectacled.screens.core

interface SyncTrigger {
    fun requestImmediate()
    fun requestImmediate(calendarIds: List<Long>)
    fun schedulePeriodic()
    fun cancel()
    fun triggerWidgetUpdate()
}

expect class PlatformSyncTrigger: SyncTrigger {
    override fun requestImmediate()
    override fun requestImmediate(calendarIds: List<Long>)
    override fun schedulePeriodic()
    override fun cancel()
    override fun triggerWidgetUpdate()
}