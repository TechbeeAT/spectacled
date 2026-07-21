package at.techbee.spectacled

import androidx.navigation.NavController
import androidx.navigation.hasRoute
import androidx.navigation.toRoute
import at.techbee.spectacled.screens.Route

/**
 * Navigate to [route] unless it is already the current top of the back stack, comparing
 * arguments too. This keeps at most one instance of the same destination on top — so overlapping
 * startup effects, repeated taps, or a re-delivered deep link can't stack a duplicate Accounts /
 * list / details screen (which would leave the back button popping onto an identical screen).
 *
 * Different arguments still stack: opening a subtask's details on top of its parent's is a
 * distinct destination, so the parent stays on the back stack and Back returns to it.
 *
 * No-op when the navController is not attached to a NavHost (landscape layout), where navigation
 * is driven by view-model state instead of the back stack.
 */
fun NavController.navigateIfNotOnTop(route: Route) {
    if (isCurrentTop(route)) return
    try {
        navigate(route)
    } catch (_: IllegalStateException) {
        // navController not attached to a NavHost yet (e.g. landscape layout) — ignore.
    }
}

private fun NavController.isCurrentTop(route: Route): Boolean {
    val current = currentBackStackEntry ?: return false
    return when (route) {
        is Route.IcalEntryList ->
            current.destination.hasRoute<Route.IcalEntryList>() && current.toRoute<Route.IcalEntryList>() == route
        is Route.IcalEntryDetails ->
            current.destination.hasRoute<Route.IcalEntryDetails>() && current.toRoute<Route.IcalEntryDetails>() == route
        is Route.AddICalEntry ->
            current.destination.hasRoute<Route.AddICalEntry>() && current.toRoute<Route.AddICalEntry>() == route
        Route.AccountsList -> current.destination.hasRoute<Route.AccountsList>()
        Route.HomeGraph -> current.destination.hasRoute<Route.HomeGraph>()
    }
}
