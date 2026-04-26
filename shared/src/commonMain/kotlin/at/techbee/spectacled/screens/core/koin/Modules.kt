package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListViewModel
import at.techbee.spectacled.screens.core.data.AppPreferences
import at.techbee.spectacled.screens.icalentry.presentation.icalentrydetails.IcalEntryDetailsViewModel
import at.techbee.spectacled.screens.icalentry.presentation.icalentrylist.IcalEntryListViewModel
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModule = module {
    viewModelOf(::IcalEntryListViewModel)
    viewModelOf(::CalendarListViewModel)
    viewModelOf(::IcalEntryDetailsViewModel)
    single { AppPreferences(Settings()) }

    includes(platformModule)
}

expect val platformModule: Module