package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.about.presentation.AboutViewModel
import at.techbee.spectacled.screens.account.presentation.calendars.AccountListViewModel
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModule = module {
    viewModelOf(::ListViewModel)
    viewModelOf(::AccountListViewModel)
    viewModelOf(::DetailsViewModel)
    viewModelOf(::AboutViewModel)

    includes(platformModule)
}

expect val platformModule: Module