package at.techbee.spectacled.screens.core

import at.techbee.spectacled.screens.core.data.ics.IcsDateTime

expect fun IcsDateTime.formatLocalized(icsDateTimeFormat: IcsDateTimeFormat): String

enum class IcsDateTimeFormat {
    DATE_TIME, DATE, TIME, DAY_OF_WEEK_SHORT, FULL_MONTH_NAME
}