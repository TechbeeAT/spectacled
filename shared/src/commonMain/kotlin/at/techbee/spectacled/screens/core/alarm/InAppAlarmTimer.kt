package at.techbee.spectacled.screens.core.alarm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Fires alarms from inside the running app, for the platforms without an OS scheduler that wakes
 * the app (desktop, web). Alarms are only fired while the app (or browser tab) is open; whatever
 * passed while it was closed is shown at the next start by [AlarmCoordinator.reconcile]'s
 * missed-alarm pass.
 *
 * Waits at most [maxSleep] at a time and then compares against the wall clock again: a computer
 * that was asleep, or a browser that throttled a background tab, would otherwise fire late.
 */
class InAppAlarmTimer(
    private val scope: CoroutineScope,
    private val now: () -> Instant = { Clock.System.now() },
    private val maxSleep: Duration = 30.seconds,
    /** Called once per alarm when it is due. */
    private val onDue: suspend (ScheduledAlarm) -> Unit
) {
    private var job: Job? = null

    /**
     * Replaces everything pending with [alarms]. Not thread-safe on its own; AlarmCoordinator
     * only calls it under its lock.
     */
    fun replaceAll(alarms: List<ScheduledAlarm>) {
        job?.cancel()
        if (alarms.isEmpty()) {
            job = null
            return
        }
        job = scope.launch {
            var pending = alarms.sortedBy { it.triggerAt }
            while (pending.isNotEmpty()) {
                val wait = pending.first().triggerAt - now()
                if (wait.isPositive()) {
                    delay(minOf(wait, maxSleep))
                    continue
                }
                val due = pending.takeWhile { it.triggerAt <= now() }
                pending = pending.drop(due.size)
                due.forEach { onDue(it) }
            }
        }
    }
}
