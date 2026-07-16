package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.about.presentation.AboutViewModel
import at.techbee.spectacled.screens.account.presentation.AccountListViewModel
import at.techbee.spectacled.screens.core.data.HttpClientFactory
import at.techbee.spectacled.screens.core.data.UserAppPreferencesStore
import at.techbee.spectacled.screens.core.data.getPlatformEngine
import at.techbee.spectacled.screens.core.data.repository.CalendarRepositoryImpl
import at.techbee.spectacled.screens.core.data.repository.IcalEntryRepositoryImpl
import at.techbee.spectacled.screens.core.data.webdav.DefaultWebDavRemoteCalendarDataSource
import at.techbee.spectacled.screens.core.data.webdav.DefaultWebDavRemoteIcalEntryDataSource
import at.techbee.spectacled.screens.core.data.webdav.WebDavRemoteCalendarDataSource
import at.techbee.spectacled.screens.core.data.webdav.WebDavRemoteIcalEntryDataSource
import at.techbee.spectacled.screens.core.domain.repository.CalendarRepository
import at.techbee.spectacled.screens.core.domain.repository.IcalEntryRepository
import at.techbee.spectacled.screens.details.presentation.DetailsViewModel
import at.techbee.spectacled.screens.list.presentation.ListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModule = module {
    single {
        val preferences: UserAppPreferencesStore = get()
        HttpClientFactory.create(
            engine = getPlatformEngine(),
            userProxyUrlProvider = { preferences.userProxyServer }
        )
    }
    singleOf(::CalendarRepositoryImpl) { bind<CalendarRepository>() }
    singleOf(::IcalEntryRepositoryImpl) { bind<IcalEntryRepository>() }

    // Stateless DAV data sources - hold only the transport (and a FileManager for entry
    // attachments); credentials are passed per call, so these can be app-wide singletons.
    single<WebDavRemoteCalendarDataSource> { DefaultWebDavRemoteCalendarDataSource(get()) }
    single<WebDavRemoteIcalEntryDataSource> { DefaultWebDavRemoteIcalEntryDataSource(get(), get()) }

    viewModelOf(::ListViewModel)
    viewModelOf(::AccountListViewModel)
    viewModelOf(::DetailsViewModel)
    viewModelOf(::AboutViewModel)

    includes(platformModule)
}

expect val platformModule: Module