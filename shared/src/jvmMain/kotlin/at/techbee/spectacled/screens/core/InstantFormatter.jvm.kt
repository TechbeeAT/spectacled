package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.toJavaInstant


actual class PlatformInstantFormatter actual constructor(val icsDateTime: IcsDateTime, val deviceZone: TimeZone) : InstantFormatter {

    actual override fun formatLocalizedDate() = formatLocalized(dateFormatter)
    actual override fun formatLocalizedTime() = formatLocalized(timeFormatter)
    actual override fun formatLocalizedDateTime() = formatLocalized(dateTimeFormatter)


    private val dateFormatter = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .withZone(icsDateTime.effectiveZone(deviceZone).toJavaZoneId())

    private val timeFormatter = DateTimeFormatter
        .ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(icsDateTime.effectiveZone(deviceZone).toJavaZoneId())

    private val dateTimeFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM,FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(icsDateTime.effectiveZone(deviceZone).toJavaZoneId())


    private fun formatLocalized(formatter: DateTimeFormatter): String {
        return if (icsDateTime.isDateOnly) {
            // Avoid accidental zone shift
            val localDate = icsDateTime.instant.toLocalDateTime(TimeZone.UTC).date
            formatter.format(localDate.atStartOfDayIn(icsDateTime.effectiveZone(deviceZone)).toJavaInstant())
        } else {
            formatter.format(icsDateTime.instant.toJavaInstant())
        }
    }
}