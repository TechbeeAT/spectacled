package at.techbee.spectacled.screens.core.data.ics

enum class KnownIcsPropertyName(val propertyName: String) {
    UID("UID"),
    DTSTAMP("DTSTAMP"),
    DTSTART("DTSTART"),
    CREATED("CREATED"),
    LASTMODIFIED("LAST-MODIFIED"),
    SUMMARY("SUMMARY"),
    DESCRIPTION("DESCRIPTION"),
    COLOR("COLOR"),
    CATEGORIES("CATEGORIES"),
    SEQUENCE("SEQUENCE")
}