package at.techbee.spectacled.screens.account.presentation.components.datastructures

import at.techbee.spectacled.screens.core.domain.CalendarComponent

enum class CalDavProvider(
    val providerName: String,
    val description: String,
    val url: String,
    val tags: List<String>,
    val supportedCalendarComponents: List<CalendarComponent>,
    val warningMessage: String? = null
) {

    MAILBOX_ORG(
        "mailbox.org",
        "Privacy-focused email, calendar, contacts, and task management hosted in Germany with excellent CalDAV support.",
        "https://mailbox.org",
        listOf(
            "\uD83C\uDDE9\uD83C\uDDEA", // Germany
            "Privacy",
            "GDPR",
            "Recommended"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    FASTMAIL(
        "Fastmail",
        "Fast and reliable email, calendar, and contact synchronization with excellent standards support.",
        "https://www.fastmail.com",
        listOf(
            "\uD83C\uDDE6\uD83C\uDDFA", // Australia
            "Power users",
            "Free trial",
            "Recommended"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    ),

    INFOMANIAK(
        "Infomaniak",
        "Swiss-hosted productivity services with strong privacy protections and excellent value.",
        "https://www.infomaniak.com",
        listOf(
            "\uD83C\uDDE8\uD83C\uDDED", // Switzerland
            "Privacy",
            "GDPR",
            "Recommended"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    MAILFENCE(
        "Mailfence",
        "Privacy-focused email and collaboration services hosted in Belgium.",
        "https://mailfence.com",
        listOf(
            "\uD83C\uDDE7\uD83C\uDDEA", // Belgium
            "Privacy",
            "GDPR"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    RUNBOX(
        "Runbox",
        "Norwegian email and collaboration platform focused on privacy and sustainability.",
        "https://runbox.com",
        listOf(
            "\uD83C\uDDF3\uD83C\uDDF4", // Norway
            "Privacy"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    MIGADU(
        "Migadu",
        "Swiss email hosting designed for custom domains and technical users.",
        "https://migadu.com",
        listOf(
            "\uD83C\uDDE8\uD83C\uDDED", // Switzerland
            "Privacy",
            "Power users"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    POSTEO(
        "Posteo",
        "Privacy-first email, calendar, and contacts provider from Germany.",
        "https://posteo.de",
        listOf(
            "\uD83C\uDDE9\uD83C\uDDEA", // Germany
            "Privacy",
            "GDPR",
            "Anonymous payments"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    NEXTCLOUD(
        "Nextcloud",
        "Host your own calendar, contacts, tasks, and notes while maintaining full control over your data.",
        "https://nextcloud.com",
        listOf(
            "Self-hosted",
            "Open Source",
            "Privacy",
            "Recommended"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    ),

    BAIKAL(
        "Baikal",
        "Lightweight open-source CalDAV and CardDAV server designed for self-hosting.",
        "https://sabre.io/baikal/",
        listOf(
            "Self-hosted",
            "Open Source",
            "Privacy"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    );

    val hasTodo = supportedCalendarComponents.contains(CalendarComponent.VTODO)
    val hasJournal = supportedCalendarComponents.contains(CalendarComponent.VJOURNAL)

}