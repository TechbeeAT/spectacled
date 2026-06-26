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

// caching the formatters as initiailzing could be expensive
private val formatterCache = mutableMapOf<String, DateTimeFormatter>()

actual fun IcsDateTime.formatLocalized(icsDateTimeFormat: IcsDateTimeFormat): String {

    val locale = Locale.getDefault()
    val zoneId = this.effectiveZone().toJavaZoneId()
    val cacheKey = "$icsDateTimeFormat-${locale.toLanguageTag()}-${zoneId.id}"

    val formatter = formatterCache.getOrPut(cacheKey) {
        when(icsDateTimeFormat) {
            IcsDateTimeFormat.DATE_TIME -> DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            IcsDateTimeFormat.DATE -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            IcsDateTimeFormat.TIME -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            IcsDateTimeFormat.DAY_OF_WEEK_SHORT -> DateTimeFormatter.ofPattern("EEE")
            IcsDateTimeFormat.FULL_MONTH_NAME -> DateTimeFormatter.ofPattern("MMMM")
        }.withLocale(locale).withZone(zoneId)
    }

    return if (this.isDateOnly) {
        // Avoid accidental zone shift
        val localDate = this.instant.toLocalDateTime(TimeZone.UTC).date
        formatter.format(localDate.atStartOfDayIn(this.effectiveZone()).toJavaInstant())
    } else {
        formatter.format(this.instant.toJavaInstant())
    }
}