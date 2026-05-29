package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.datetime.TimeZone

interface InstantFormatter {
    fun formatLocalizedDateTime(): String
    fun formatLocalizedDate(): String
    fun formatLocalizedTime(): String
    fun formatLocalizedDayOfWeekShort(): String
    fun formatFullMonthName(): String
}

expect class PlatformInstantFormatter(icsDateTime: IcsDateTime, deviceZone: TimeZone = TimeZone.currentSystemDefault()): InstantFormatter {
    override fun formatLocalizedDateTime(): String
    override fun formatLocalizedDate(): String
    override fun formatLocalizedTime(): String
    override fun formatLocalizedDayOfWeekShort(): String
    override fun formatFullMonthName(): String
}