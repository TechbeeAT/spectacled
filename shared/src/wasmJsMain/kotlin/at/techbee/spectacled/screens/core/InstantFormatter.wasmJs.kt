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
actual class PlatformInstantFormatter actual constructor(
    private val icsDateTime: IcsDateTime,
    private val deviceZone: TimeZone
) : InstantFormatter {

    private val effectiveZone: TimeZone =
        icsDateTime.effectiveZone()

    private val timeZoneId: String =
        when {
            icsDateTime.isDateOnly -> "UTC"
            else -> effectiveZone.id
        }

    private fun format(
        formatterFactory: (String, String) -> JsAny
    ): String {

        val lang = getNavigatorLanguage()
        val formatter = formatterFactory(lang, timeZoneId)

        val millis = if (icsDateTime.isDateOnly) {

            // Prevent day shift for all-day
            val localDate = icsDateTime.instant
                .toLocalDateTime(TimeZone.UTC)
                .date

            localDate
                .atStartOfDayIn(effectiveZone)
                .toEpochMilliseconds()

        } else {
            icsDateTime.instant.toEpochMilliseconds()
        }

        return formatWithFormatter(formatter, millis.toDouble())
    }

    actual override fun formatLocalizedDateTime(): String =
        format(::createDateTimeFormatter)

    actual override fun formatLocalizedDate(): String =
        format(::createDateFormatter)

    actual override fun formatLocalizedTime(): String =
        format(::createTimeFormatter)

    actual override fun formatLocalizedDayOfWeekShort(): String =
        format(::createDayOfWeekFormatter)

    actual override fun formatFullMonthName(): String =
        format(::createMonthNameFormatter)
}