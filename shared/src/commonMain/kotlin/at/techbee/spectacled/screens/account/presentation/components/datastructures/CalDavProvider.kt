package at.techbee.spectacled.screens.account.presentation.components.datastructures

import at.techbee.spectacled.screens.core.domain.CalendarComponent
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.provider_baikal_description
import spectacled.shared.generated.resources.provider_category_email
import spectacled.shared.generated.resources.provider_category_nextcloud
import spectacled.shared.generated.resources.provider_category_self_hosted
import spectacled.shared.generated.resources.provider_disroot_description
import spectacled.shared.generated.resources.provider_fastmail_description
import spectacled.shared.generated.resources.provider_flag_australia
import spectacled.shared.generated.resources.provider_flag_belgium
import spectacled.shared.generated.resources.provider_flag_european_union
import spectacled.shared.generated.resources.provider_flag_france
import spectacled.shared.generated.resources.provider_flag_germany
import spectacled.shared.generated.resources.provider_flag_netherlands
import spectacled.shared.generated.resources.provider_flag_switzerland
import spectacled.shared.generated.resources.provider_hetzner_description
import spectacled.shared.generated.resources.provider_infomaniak_description
import spectacled.shared.generated.resources.provider_mailfence_description
import spectacled.shared.generated.resources.provider_murena_description
import spectacled.shared.generated.resources.provider_nextcloud_description
import spectacled.shared.generated.resources.provider_posteo_description
import spectacled.shared.generated.resources.provider_radicale_description
import spectacled.shared.generated.resources.provider_tabdigital_description
import spectacled.shared.generated.resources.provider_tag_anonymous_payments
import spectacled.shared.generated.resources.provider_tag_free_plan
import spectacled.shared.generated.resources.provider_tag_free_trial
import spectacled.shared.generated.resources.provider_tag_gdpr
import spectacled.shared.generated.resources.provider_tag_nextcloud
import spectacled.shared.generated.resources.provider_tag_open_source
import spectacled.shared.generated.resources.provider_tag_power_users
import spectacled.shared.generated.resources.provider_tag_privacy
import spectacled.shared.generated.resources.provider_tag_self_hosted
import spectacled.shared.generated.resources.provider_woelkli_description

enum class CalDavProviderCategory(val headline: StringResource) {
    NEXTCLOUD(Res.string.provider_category_nextcloud),
    SELF_HOSTED(Res.string.provider_category_self_hosted),
    EMAIL(Res.string.provider_category_email)
}

enum class CalDavProvider(
    val providerName: String,
    val description: StringResource,
    val url: String,
    val tags: List<StringResource>,
    val supportedCalendarComponents: List<CalendarComponent>,
    val category: CalDavProviderCategory
) {
    MURENA(
        "Murena Workspace",
        Res.string.provider_murena_description,
        "https://murena.com/workspace/partner/techbee/",
        listOf(
            Res.string.provider_flag_france,
            Res.string.provider_tag_nextcloud,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_gdpr,
            Res.string.provider_tag_free_plan
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.NEXTCLOUD
    ),

    TABDIGITAL(
        "Tab.Digital",
        Res.string.provider_tabdigital_description,
        "https://tab.digital",
        listOf(
            Res.string.provider_flag_european_union,
            Res.string.provider_tag_nextcloud,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_gdpr,
            Res.string.provider_tag_free_plan
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.NEXTCLOUD
    ),

    HETZNER(
        "Hetzner Storage Share",
        Res.string.provider_hetzner_description,
        "https://www.hetzner.com/storage/storage-share/",
        listOf(
            Res.string.provider_flag_germany,
            Res.string.provider_tag_nextcloud,
            Res.string.provider_tag_gdpr,
            Res.string.provider_tag_power_users
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.NEXTCLOUD
    ),

    WOELKLI(
        "woelkli",
        Res.string.provider_woelkli_description,
        "https://woelkli.com",
        listOf(
            Res.string.provider_flag_switzerland,
            Res.string.provider_tag_nextcloud,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_free_plan
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.NEXTCLOUD
    ),

    DISROOT(
        "Disroot",
        Res.string.provider_disroot_description,
        "https://disroot.org",
        listOf(
            Res.string.provider_flag_netherlands,
            Res.string.provider_tag_nextcloud,
            Res.string.provider_tag_open_source,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_free_plan
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.NEXTCLOUD
    ),

    NEXTCLOUD(
        "Nextcloud",
        Res.string.provider_nextcloud_description,
        "https://nextcloud.com",
        listOf(
            Res.string.provider_tag_self_hosted,
            Res.string.provider_tag_open_source,
            Res.string.provider_tag_privacy
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.SELF_HOSTED
    ),

    BAIKAL(
        "Baikal",
        Res.string.provider_baikal_description,
        "https://sabre.io/baikal/",
        listOf(
            Res.string.provider_tag_self_hosted,
            Res.string.provider_tag_open_source,
            Res.string.provider_tag_privacy
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.SELF_HOSTED
    ),

    RADICALE(
        "Radicale",
        Res.string.provider_radicale_description,
        "https://radicale.org",
        listOf(
            Res.string.provider_tag_self_hosted,
            Res.string.provider_tag_open_source,
            Res.string.provider_tag_privacy
        ),
        listOf(
            CalendarComponent.VTODO,
            CalendarComponent.VJOURNAL
        ),
        CalDavProviderCategory.SELF_HOSTED
    ),
/*
    MAILBOX_ORG(
        "mailbox.org",
        Res.string.provider_mailbox_org_description,
        "https://mailbox.org",
        listOf(
            Res.string.provider_flag_germany,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_gdpr
        ),
        listOf(CalendarComponent.VTODO),
        CalDavProviderCategory.EMAIL
    ),

 */

    FASTMAIL(
        "Fastmail",
        Res.string.provider_fastmail_description,
        "https://www.fastmail.com",
        listOf(
            Res.string.provider_flag_australia,
            Res.string.provider_tag_power_users,
            Res.string.provider_tag_free_trial
        ),
        listOf(CalendarComponent.VTODO),
        CalDavProviderCategory.EMAIL
    ),

    INFOMANIAK(
        "Infomaniak",
        Res.string.provider_infomaniak_description,
        "https://www.infomaniak.com",
        listOf(
            Res.string.provider_flag_switzerland,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_gdpr
        ),
        listOf(CalendarComponent.VTODO),
        CalDavProviderCategory.EMAIL
    ),

    MAILFENCE(
        "Mailfence",
        Res.string.provider_mailfence_description,
        "https://mailfence.com",
        listOf(
            Res.string.provider_flag_belgium,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_gdpr
        ),
        listOf(CalendarComponent.VTODO),
        CalDavProviderCategory.EMAIL
    ),
/*
    RUNBOX(
        "Runbox",
        Res.string.provider_runbox_description,
        "https://runbox.com",
        listOf(
            Res.string.provider_flag_norway,
            Res.string.provider_tag_privacy
        ),
        listOf(CalendarComponent.VTODO),
        CalDavProviderCategory.EMAIL
    ),

    MIGADU(
        "Migadu",
        Res.string.provider_migadu_description,
        "https://migadu.com",
        listOf(
            Res.string.provider_flag_switzerland,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_power_users
        ),
        listOf(CalendarComponent.VTODO),
        CalDavProviderCategory.EMAIL
    ),

 */

    POSTEO(
        "Posteo",
        Res.string.provider_posteo_description,
        "https://posteo.de",
        listOf(
            Res.string.provider_flag_germany,
            Res.string.provider_tag_privacy,
            Res.string.provider_tag_gdpr,
            Res.string.provider_tag_anonymous_payments
        ),
        listOf(CalendarComponent.VTODO),
        CalDavProviderCategory.EMAIL
    )
    ;

    val hasTodo = supportedCalendarComponents.contains(CalendarComponent.VTODO)
    val hasJournal = supportedCalendarComponents.contains(CalendarComponent.VJOURNAL)

}
