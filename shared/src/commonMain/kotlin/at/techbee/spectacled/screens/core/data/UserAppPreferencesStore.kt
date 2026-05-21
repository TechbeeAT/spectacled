package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import at.techbee.spectacled.theme.ThemeOption

const val APP_PREFERENCES_FILE_NAME = "app_preferences"

const val LAST_USED_CALENDAR_ID = "last_used_calendar_id"
const val LIST_SORTED_BY = "list_sorted_by"
const val LIST_SORTED_BY_ASCENDING = "list_sorted_by_ascending"
const val LIST_LAYOUT = "list_layout"
const val LIST_COLLAPSED_GROUPS = "list_collapsed_groups"
const val LIST_COLLAPSED_GROUP_TRASHBIN = "list_collapsed_group_trashbin"
const val LIST_COLLAPSED_GROUP_PINNED = "list_collapsed_group_pinned"

const val THEME_OPTION = "theme_option"

interface UserAppPreferencesStore {
    fun save(key: String, value: String)
    fun load(key: String): String?
    fun remove(key: String)


    var lastUsedCalendarId: Long?
        get() = this.load(LAST_USED_CALENDAR_ID)?.toLongOrNull()
        set(value) {
            if (value == null) this.remove(LAST_USED_CALENDAR_ID)
            else this.save(LAST_USED_CALENDAR_ID, value.toString())
        }

    var listSortedBy: ListSortedBy?
        get() = this.load(LIST_SORTED_BY)?.let { savedSortedBy -> ListSortedBy.entries.find { savedSortedBy == it.name } }
        set(value) {
            if (value == null) this.remove(LIST_SORTED_BY)
            else this.save(LIST_SORTED_BY, value.name)
        }

    var listSortedByAscending: Boolean
        get() = this.load(LIST_SORTED_BY_ASCENDING)?.toBooleanStrictOrNull() ?: true
        set(value) = this.save(LIST_SORTED_BY_ASCENDING, if(value) "true" else "false")

    var listLayout: ListLayout?
        get() = this.load(LIST_LAYOUT)?.let { savedLayout -> ListLayout.entries.find { savedLayout == it.name } }
        set(value) {
            if (value == null) this.remove(LIST_LAYOUT)
            else this.save(LIST_LAYOUT, value.name)
        }

    var listCollapsedGroups: Set<String>
        get() = this.load(LIST_COLLAPSED_GROUPS)
            ?.split("|")?.toSet() ?: emptySet()
        set(value) = this.save(LIST_COLLAPSED_GROUPS, value.joinToString("|"))

    var themeOption: ThemeOption
        get() = this.load(THEME_OPTION)?.let { ThemeOption.entries.find { themeOption -> themeOption.name == it } } ?: ThemeOption.SYSTEM
        set(value) = this.save(THEME_OPTION, value.name)
}

expect class PlatformUserAppPreferencesStore: UserAppPreferencesStore {

    override fun save(key: String, value: String)
    override fun load(key: String): String?
    override fun remove(key: String)
}
