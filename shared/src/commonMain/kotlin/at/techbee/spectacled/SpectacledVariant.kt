package at.techbee.spectacled

import androidx.compose.ui.graphics.Color
import at.techbee.spectacled.screens.core.domain.CalendarComponent
import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import at.techbee.spectacled.theme.ThemeFont
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import spectacled.shared.generated.resources.Res
import spectacled.shared.generated.resources.add_journal
import spectacled.shared.generated.resources.add_note
import spectacled.shared.generated.resources.add_task
import spectacled.shared.generated.resources.app_name_spectacled_journals
import spectacled.shared.generated.resources.app_name_spectacled_notes
import spectacled.shared.generated.resources.app_name_spectacled_tasks
import spectacled.shared.generated.resources.ic_add_journal
import spectacled.shared.generated.resources.ic_add_note
import spectacled.shared.generated.resources.ic_add_task
import spectacled.shared.generated.resources.logo_spectacled_journals
import spectacled.shared.generated.resources.logo_spectacled_notes
import spectacled.shared.generated.resources.logo_spectacled_tasks

enum class SpectacledVariant(
    val dbName: String,
    val appNameStringRes: StringResource,
    val logoDrawableResource: DrawableResource,
    val addNewStringRes: StringResource,
    val addNewDrawableRes: DrawableResource,
    val mainCalendarComponent: CalendarComponent,
    val themeSeedColor: Color,
    val deeplinkUriScheme: String,
    val deeplinkWebUri: String,
    val defaultListSortedBy: ListSortedBy,
    val defaultListLayout: ListLayout,
    val defaultThemeFont: ThemeFont
) {

    JOURNALS(
        "spectacled_journals.db",
        Res.string.app_name_spectacled_journals,
        Res.drawable.logo_spectacled_journals,
        Res.string.add_journal,
        Res.drawable.ic_add_journal,
        CalendarComponent.VJOURNAL,
        Color(0, 104, 150),
        "spectacled-journals",
        "https://spectacled.techbee.at/journals",
        ListSortedBy.DATE,
        ListLayout.LIST,
        ThemeFont.NOTO_SERIF
    ),
    NOTES(
        "spectacled_notes.db",
        Res.string.app_name_spectacled_notes,
        Res.drawable.logo_spectacled_notes,
        Res.string.add_note,
        Res.drawable.ic_add_note,
        CalendarComponent.VJOURNAL,
        Color(153, 76, 44),
        "spectacled-notes",
        "https://spectacled.techbee.at/notes",
        ListSortedBy.LAST_MODIFIED,
        ListLayout.STAGGERED_GRID,
        ThemeFont.ABEEZEE

    ),
    TASKS(
        "spectacled_tasks.db",
        Res.string.app_name_spectacled_tasks,
        Res.drawable.logo_spectacled_tasks,
        Res.string.add_task,
        Res.drawable.ic_add_task,
        CalendarComponent.VTODO,
        Color(41, 111, 35),
        "spectacled-tasks",
        "https://spectacled.techbee.at/tasks",
        ListSortedBy.DRAGANDDROP,
        ListLayout.LIST,
        ThemeFont.ROBOTO
    );
}