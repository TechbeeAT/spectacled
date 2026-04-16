package at.techbee.spectacled.screens.core.koin

import at.techbee.spectacled.screens.account.presentation.calendars.CalendarListViewModel
import at.techbee.spectacled.screens.core.data.AppPreferences
import at.techbee.spectacled.screens.note.presentation.notedetails.NoteDetailsViewModel
import at.techbee.spectacled.screens.note.presentation.notelist.NoteListViewModel
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModule = module {
    viewModelOf(::NoteListViewModel)
    viewModelOf(::CalendarListViewModel)
    viewModelOf(::NoteDetailsViewModel)
    single { AppPreferences(Settings()) }

    includes(platformModule)
}

expect val platformModule: Module