package at.techbee.spectacled.screens.core.data

import at.techbee.spectacled.screens.list.presentation.datastructures.ListLayout
import at.techbee.spectacled.screens.list.presentation.datastructures.ListSortedBy
import com.russhwolf.settings.Settings

//const val APP_PREFERENCES_NAME = "app_preferences"
const val LAST_USED_CALENDAR_ID = "last_used_calendar_id"
const val LIST_SORTED_BY = "list_sorted_by"
const val LIST_SORTED_BY_ASCENDING = "list_sorted_by_ascending"
const val LIST_LAYOUT = "list_layout"
const val LIST_COLLAPSED_GROUPS = "list_collapsed_groups"
const val LIST_COLLAPSED_GROUP_TRASHBIN = "list_collapsed_group_trashbin"
const val LIST_COLLAPSED_GROUP_PINNED = "list_collapsed_group_pinned"



class AppPreferences(private val settings: Settings) {
    // simple getter/setter style
    /*
    var onboardingShown: Boolean
        get() = settings.getBoolean("onboarding_shown", defaultValue = false)
        set(value) = settings.putBoolean("onboarding_shown", value)

    var accessToken: String?
        get() = settings.getStringOrNull("access_token")
        set(value) {
            if (value == null) settings.remove("access_token")
            else settings.putString("access_token", value)
        }
     */

    var lastUsedCalendarId: Long?
        get() = settings.getLongOrNull(LAST_USED_CALENDAR_ID)
        set(value) {
            if (value == null) settings.remove(LAST_USED_CALENDAR_ID)
            else settings.putLong(LAST_USED_CALENDAR_ID, value)
        }

    var listSortedBy: ListSortedBy?
        get() = settings.getStringOrNull(LIST_SORTED_BY)?.let { savedSortedBy -> ListSortedBy.entries.find { savedSortedBy == it.name } }
        set(value) {
            if (value == null) settings.remove(LIST_SORTED_BY)
            else settings.putString(LIST_SORTED_BY, value.name)
        }

    var listSortedByAscending: Boolean
        get() = settings.getBoolean(LIST_SORTED_BY_ASCENDING, defaultValue = true)
        set(value) = settings.putBoolean(LIST_SORTED_BY_ASCENDING, value)

    var listLayout: ListLayout?
        get() = settings.getStringOrNull(LIST_LAYOUT)?.let { savedLayout -> ListLayout.entries.find { savedLayout == it.name } }
        set(value) {
            if (value == null) settings.remove(LIST_LAYOUT)
            else settings.putString(LIST_LAYOUT, value.name)
        }

    var listCollapsedGroups: Set<String>
        get() = settings.getStringOrNull(LIST_COLLAPSED_GROUPS)
            ?.split("|")?.toSet() ?: emptySet()
        set(value) = settings.putString(LIST_COLLAPSED_GROUPS, value.joinToString("|"))

}
