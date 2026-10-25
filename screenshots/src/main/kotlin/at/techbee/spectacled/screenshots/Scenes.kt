package at.techbee.spectacled.screenshots

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.details.presentation.DetailsAction
import at.techbee.spectacled.screens.details.presentation.DetailsSheetOrDialog
import at.techbee.spectacled.screens.list.presentation.ListAction
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy

/**
 * One screenshot: [setup] opens a screen and then sends it the same actions the UI would send,
 * so bottom sheets, dialogs, filters or sort orders are opened exactly like a tap would.
 * Entries are referenced by their uid in sample-data/screenshot_entries.sql.
 */
internal class Scene(
    val name: String,
    val setup: suspend SceneScope.() -> Unit
)

/** The screenshots per app, in store order. The file name is the scene name. */
internal fun scenesFor(variant: SpectacledVariant): List<Scene> = when (variant) {

    SpectacledVariant.JOURNALS -> listOf(
        Scene("01_list") { openList() },
        Scene("02_entry") { openDetails("sample-journal-diary-2") },
        Scene("03_meeting_minutes") { openDetails("sample-journal-minutes-1") },
        Scene("04_categories") {
            openDetails("sample-journal-diary-1")
            details(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.CATEGORY_SELECTOR))
        },
        Scene("05_filter") {
            openList()
            list(ListAction.OnToggleFilterBar)
        },
        Scene("06_accounts") { openAccounts() },
    )

    SpectacledVariant.NOTES -> listOf(
        Scene("01_list") { openList() },
        Scene("02_recipe") { openDetails("sample-note-recipe") },
        Scene("03_color") {
            openDetails("sample-note-ideas")
            details(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.COLOR_SELECTOR))
        },
        Scene("04_accounts") { openAccounts() },
    )

    SpectacledVariant.TASKS -> listOf(
        Scene("01_list") {
            openList()
            list(ListAction.OnSortedByChanged(ListSortedBy.DUE, listSortedByAscending = false))
        },
        Scene("02_project") { openDetails("sample-task-website-relaunch") },
        Scene("03_status") {
            openDetails("sample-task-plan-japan")
            details(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.TASK_STATUS_PICKER))
        },
        Scene("04_accounts") { openAccounts() },
    )
}
