package at.techbee.spectacled.screens.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import at.techbee.spectacled.screens.Route

/**
 * Single source of truth for the current navigation location, shared by the
 * portrait ([at.techbee.spectacled.screens.PortraitLayout]) and landscape
 * ([at.techbee.spectacled.screens.LandscapeLayout]) presentations.
 *
 * It is a [ViewModel] so the stack survives configuration changes and, because
 * both layouts read and write the *same* instance, switching between them no
 * longer resets the open screen. The `NavController` used by the portrait layout
 * is a pure projection of this stack and never a source of truth.
 *
 * Stack shape: [Route.AccountsList] is always the root. An [Route.IcalEntryList]
 * sits directly above it (selecting a calendar replaces any open list and its
 * details), and a detail destination ([Route.IcalEntryDetails] /
 * [Route.AddICalEntry]) sits on top.
 */
class AppNavigator : ViewModel() {

    val backStack = mutableStateListOf<Route>(Route.AccountsList)

    val current: Route get() = backStack.last()

    /** True when there is a destination above the [Route.AccountsList] root. */
    val canNavigateUp: Boolean get() = backStack.size > 1

    fun navigate(route: Route) {
        when (route) {
            // Graph container, never an actual destination.
            is Route.HomeGraph -> return
            // Returning to the accounts root collapses the whole stack.
            is Route.AccountsList -> backStack.retainAll { it is Route.AccountsList }
            // A list lives directly above the accounts root; opening one replaces
            // any currently open list and its details.
            is Route.IcalEntryList -> {
                backStack.retainAll { it is Route.AccountsList }
                backStack.add(route)
            }
            else -> if (backStack.lastOrNull() != route) backStack.add(route)
        }
    }

    /** Pops the top destination. Returns false when already at the root. */
    fun navigateUp(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }
}
