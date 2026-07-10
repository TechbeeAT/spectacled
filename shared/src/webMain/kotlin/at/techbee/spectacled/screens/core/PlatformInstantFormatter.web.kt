package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "((lang, timeZone) => new Intl.DateTimeFormat(lang, { dateStyle: 'medium', timeStyle: 'short', timeZone }))"
)
private external fun createDateTimeFormatter(lang: String, timeZone: String): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "((lang, timeZone) => new Intl.DateTimeFormat(lang, { dateStyle: 'medium', timeZone }))"
)
private external fun createDateFormatter(lang: String, timeZone: String): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "((lang, timeZone) => new Intl.DateTimeFormat(lang, { timeStyle: 'short', timeZone }))"
)
private external fun createTimeFormatter(lang: String, timeZone: String): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "((lang, timeZone) => new Intl.DateTimeFormat(lang, { weekday: 'short', timeZone }))"
)
private external fun createDayOfWeekFormatter(lang: String, timeZone: String): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "((lang, timeZone) => new Intl.DateTimeFormat(lang, { month: 'long', timeZone }))"
)
private external fun createMonthNameFormatter(lang: String, timeZone: String): JsAny

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(formatter, millis) => formatter.format(new Date(millis))")
private external fun formatWithFormatter(formatter: JsAny, millis: Double): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => navigator.language")
private external fun getNavigatorLanguage(): String




@OptIn(ExperimentalWasmJsInterop::class)
private val formatterCache = mutableMapOf<String, JsAny>()

@OptIn(ExperimentalWasmJsInterop::class)
actual fun IcsDateTime.formatLocalized(icsDateTimeFormat: IcsDateTimeFormat): String {

    val lang = getNavigatorLanguage()
    val effectiveZone = this.effectiveZone()
    val timeZoneId = if (this.isDateOnly) "UTC" else effectiveZone.id

    val cacheKey = "$icsDateTimeFormat-$lang-$timeZoneId"

    val formatter = formatterCache.getOrPut(cacheKey) {
        when(icsDateTimeFormat) {
            IcsDateTimeFormat.DATE_TIME -> createDateTimeFormatter(lang, timeZoneId)
            IcsDateTimeFormat.DATE -> createDateFormatter(lang, timeZoneId)
            IcsDateTimeFormat.TIME -> createTimeFormatter(lang, timeZoneId)
            IcsDateTimeFormat.DAY_OF_WEEK_SHORT -> createDayOfWeekFormatter(lang, timeZoneId)
            IcsDateTimeFormat.FULL_MONTH_NAME -> createMonthNameFormatter(lang, timeZoneId)
        }
    }

    val millis = if (this.isDateOnly) {
        this.instant.toLocalDateTime(TimeZone.UTC).date
            .atStartOfDayIn(effectiveZone)
            .toEpochMilliseconds()
    } else {
        this.instant.toEpochMilliseconds()
    }

    return formatWithFormatter(formatter, millis.toDouble())
}