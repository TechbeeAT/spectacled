package at.techbee.spectacled.screenshots

import at.techbee.spectacled.SpectacledVariant
import at.techbee.spectacled.screens.account.presentation.AccountListAction
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
internal fun scenesFor(variant: SpectacledVariant): List<Scene> {
    // 02 shows the first entry, 03 a different one with its categories open.
    val (entryUid, categoriesEntryUid) = when (variant) {
        SpectacledVariant.JOURNALS -> "sample-journal-diary-2" to "sample-journal-minutes-1"
        SpectacledVariant.NOTES -> "sample-note-recipe" to "sample-note-ideas"
        SpectacledVariant.TASKS -> "sample-task-website-relaunch" to "sample-task-plan-japan"
    }

    // 07 shows a third entry with the app's own picker sheet open.
    val (pickerEntryUid, pickerSheet) = when (variant) {
        SpectacledVariant.JOURNALS -> "sample-journal-diary-1" to DetailsSheetOrDialog.JOURNAL_STATUS_PICKER
        SpectacledVariant.NOTES -> "sample-note-gifts" to DetailsSheetOrDialog.COLOR_SELECTOR
        SpectacledVariant.TASKS -> "sample-task-passport" to DetailsSheetOrDialog.TASK_STATUS_PICKER
    }

    return listOf(
        Scene("01_list") {
            openList()
            if (variant == SpectacledVariant.TASKS)
                list(ListAction.OnSortedByChanged(ListSortedBy.DUE, listSortedByAscending = false))
        },
        Scene("02_entry") { openDetails(entryUid) },
        Scene("03_categories") {
            openDetails(categoriesEntryUid)
            details(DetailsAction.OnShowSheetOrDialog(DetailsSheetOrDialog.CATEGORY_SELECTOR))
        },
        Scene("04_accounts") { openAccounts() },
        Scene("05_add_account") {
            openAccounts()
            accounts(AccountListAction.OnShowAddPrincipalBottomSheet(true))
        },
        Scene("06_filter") {
            openList()
            list(ListAction.OnToggleFilterBar)
        },
        Scene("07_picker") {
            openDetails(pickerEntryUid)
            details(DetailsAction.OnShowSheetOrDialog(pickerSheet))
        },
    )
}
