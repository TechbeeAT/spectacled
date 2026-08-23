package at.techbee.spectacled.screens.account.presentation.components.datastructures

import at.techbee.spectacled.screens.core.domain.CalendarComponent

enum class CalDavProvider(
    val providerName: String,
    val description: String,
    val url: String,
    val tags: List<String>,
    val supportedCalendarComponents: List<CalendarComponent>,
    val recommended: Boolean = false,
    val warningMessage: String? = null
) {

    TABDIGITAL(
        "Tab.Digital",
        "Managed Nextcloud hosting in the EU with calendars, tasks, journals, contacts, and files. Offers a free account to get started without a credit card.",
        "https://tab.digital",
        listOf(
            "🇪🇺", // European Union
            "Nextcloud",
            "Privacy",
            "GDPR",
            "Free plan"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        recommended = true
    ),

    MURENA(
        "Murena Workspace",
        "Privacy-focused, deGoogled online workspace from France built on Nextcloud and OnlyOffice, including calendars, tasks, journals, and contacts, with a free tier to get started.",
        "https://murena.com/workspace/",
        listOf(
            "🇫🇷", // France
            "Nextcloud",
            "Privacy",
            "GDPR",
            "Free plan"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    ),

    HETZNER(
        "Hetzner Storage Share",
        "Managed Nextcloud hosting from Germany with full calendar, tasks, journals, contacts, and file synchronisation.",
        "https://www.hetzner.com/storage/storage-share/",
        listOf(
            "🇩🇪", // Germany
            "Nextcloud",
            "GDPR",
            "Power users"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    ),

    WOELKLI(
        "woelkli",
        "Secure Nextcloud-based cloud storage hosted in Switzerland, including calendars, tasks, journals, and contacts. Includes a free tier.",
        "https://woelkli.com",
        listOf(
            "🇨🇭", // Switzerland
            "Nextcloud",
            "Privacy",
            "Free plan"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    ),

    DISROOT(
        "Disroot",
        "Community-run, donation-funded platform from the Netherlands built on Nextcloud, offering calendars, tasks, journals, and more for free.",
        "https://disroot.org",
        listOf(
            "🇳🇱", // Netherlands
            "Nextcloud",
            "Open Source",
            "Privacy",
            "Free plan"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    ),

    NEXTCLOUD(
        "Nextcloud",
        "Host your own calendar, contacts, tasks, and notes while maintaining full control over your data.",
        "https://nextcloud.com",
        listOf(
            "Self-hosted",
            "Open Source",
            "Privacy"
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
    ),

    RADICALE(
        "Radicale",
        "Lightweight open-source CalDAV and CardDAV server designed for self-hosting, supporting events, tasks, and journals.",
        "https://radicale.org",
        listOf(
            "Self-hosted",
            "Open Source",
            "Privacy"
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        )
    ),

    MAILBOX_ORG(
        "mailbox.org",
        "Privacy-focused email, calendar, contacts, and task management hosted in Germany with excellent CalDAV support.",
        "https://mailbox.org",
        listOf(
            "🇩🇪", // Germany
            "Privacy",
            "GDPR"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    FASTMAIL(
        "Fastmail",
        "Fast and reliable email, calendar, and contact synchronization with excellent standards support.",
        "https://www.fastmail.com",
        listOf(
            "🇦🇺", // Australia
            "Power users",
            "Free trial"
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
            "🇨🇭", // Switzerland
            "Privacy",
            "GDPR"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    MAILFENCE(
        "Mailfence",
        "Privacy-focused email and collaboration services hosted in Belgium.",
        "https://mailfence.com",
        listOf(
            "🇧🇪", // Belgium
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
            "🇳🇴", // Norway
            "Privacy"
        ),
        listOf(CalendarComponent.VTODO)
    ),

    MIGADU(
        "Migadu",
        "Swiss email hosting designed for custom domains and technical users.",
        "https://migadu.com",
        listOf(
            "🇨🇭", // Switzerland
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
            "🇩🇪", // Germany
            "Privacy",
            "GDPR",
            "Anonymous payments"
        ),
        listOf(CalendarComponent.VTODO)
    );

    val hasTodo = supportedCalendarComponents.contains(CalendarComponent.VTODO)
    val hasJournal = supportedCalendarComponents.contains(CalendarComponent.VJOURNAL)

}
