package com.freeletics.mad.navigator.compose

import androidx.annotation.IdRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavDestination as AndroidXNavDestination
import androidx.navigation.compose.NavHost as AndroidXNavHost
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import androidx.navigation.get
import com.freeletics.mad.navigator.NavRoot
import com.freeletics.mad.navigator.NavRoute
import com.freeletics.mad.navigator.compose.NavDestination.Activity
import com.freeletics.mad.navigator.compose.NavDestination.BottomSheet
import com.freeletics.mad.navigator.compose.NavDestination.Dialog
import com.freeletics.mad.navigator.compose.NavDestination.RootScreen
import com.freeletics.mad.navigator.compose.NavDestination.Screen
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator

/**
 * Create a new [androidx.navigation.compose.NavHost] with a [androidx.navigation.NavGraph]
 * containing all given [destinations]. [startRoot] will be used as the start destination
 * of the graph.
 *
 * [destinationCreator] can be passed to add support for custom subclasses of [NavDestination].
 */
@ExperimentalMaterialNavigationApi
@Composable
public fun NavHost(
    startRoot: NavRoot,
    destinations: Set<NavDestination>,
    destinationCreator: (NavDestination) -> AndroidXNavDestination? = { null },
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val startDestinationId = startRoot.destinationId
    NavHost(navController, startDestinationId, destinations, destinationCreator)
}

/**
 * Create a new [androidx.navigation.compose.NavHost] with a [androidx.navigation.NavGraph]
 * containing all given [destinations]. [startRoute] will be used as the start destination
 * of the graph.
 *
 * [destinationCreator] can be passed to add support for custom subclasses of [NavDestination].
 */
@ExperimentalMaterialNavigationApi
@Composable
public fun NavHost(
    startRoute: NavRoute,
    destinations: Set<NavDestination>,
    destinationCreator: (NavDestination) -> AndroidXNavDestination? = { null },
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val startDestinationId = startRoute.destinationId
    NavHost(navController, startDestinationId, destinations, destinationCreator)
}

/**
 * Create a new [androidx.navigation.compose.NavHost] using [navController] with a
 * [androidx.navigation.NavGraph] containing all given [destinations]. [startRoot] will be used as
 * the start destination of the graph.
 *
 * To support [NavDestination.BottomSheet] the given [navController] needs to contain a navigator
 * created with [rememberBottomSheetNavigator].
 *
 * [destinationCreator] can be passed to add support for custom subclasses of [NavDestination].
 */
@Composable
public fun NavHost(
    navController: NavHostController,
    startRoot: NavRoot,
    destinations: Set<NavDestination>,
    destinationCreator: (NavDestination) -> AndroidXNavDestination? = { null },
) {
    val startDestinationId = startRoot.destinationId
    NavHost(navController, startDestinationId, destinations, destinationCreator)
}

/**
 * Create a new [androidx.navigation.compose.NavHost] using [navController] with a
 * [androidx.navigation.NavGraph] containing all given [destinations]. [startRoute] will be used as
 * the start destination of the graph.
 *
 * To support [NavDestination.BottomSheet] the given [navController] needs to contain a navigator
 * created with [rememberBottomSheetNavigator].
 *
 * [destinationCreator] can be passed to add support for custom subclasses of [NavDestination].
 */
@Composable
public fun NavHost(
    navController: NavHostController,
    startRoute: NavRoute,
    destinations: Set<NavDestination>,
    destinationCreator: (NavDestination) -> AndroidXNavDestination? = { null },
) {
    val startDestinationId = startRoute.destinationId
    NavHost(navController, startDestinationId, destinations, destinationCreator)
}

@Composable
private fun NavHost(
    navController: NavHostController,
    @IdRes startDestinationId: Int,
    destinations: Set<NavDestination>,
    destinationCreator: (NavDestination) -> AndroidXNavDestination?,
) {
    val graph = remember(navController, startDestinationId, destinations, destinationCreator) {
        @Suppress("deprecation")
        navController.createGraph(startDestination = startDestinationId) {
            destinations.forEach { destination ->
                addDestination(navController, destination, destinationCreator)
            }
        }
    }
    AndroidXNavHost(navController, graph)
}

// the BottomSheet class and creator methods are marked with ExperimentalMaterialNavigationApi
// if those stay unused the experimental code is never called, so we swallow the warning here
@OptIn(ExperimentalMaterialNavigationApi::class)
private fun NavGraphBuilder.addDestination(
    controller: NavController,
    destination: NavDestination,
    destinationCreator: (NavDestination) -> AndroidXNavDestination?,
) {
    val newDestination = when (destination) {
        is Screen -> destination.toDestination(controller)
        is RootScreen -> destination.toDestination(controller)
        is Dialog -> destination.toDestination(controller)
        is BottomSheet -> destination.toDestination(controller)
        is Activity -> destination.toDestination(controller)
        else -> destinationCreator(destination)
    } ?: throw IllegalArgumentException("Unable to create destination for unknown type " +
        "${destination::class.java}. Handle it in destinationCreator")

    addDestination(newDestination)
}

private fun Screen.toDestination(
    controller: NavController,
): ComposeNavigator.Destination {
    val navigator = controller.navigatorProvider[ComposeNavigator::class]
    return ComposeNavigator.Destination(navigator) {
        screenContent(controller)
    }.also {
        it.id = destinationId
    }
}

private fun RootScreen.toDestination(
    controller: NavController,
): ComposeNavigator.Destination {
    val navigator = controller.navigatorProvider[ComposeNavigator::class]
    return ComposeNavigator.Destination(navigator) { screenContent(controller) }.also {
        it.id = destinationId
    }
}

private fun Dialog.toDestination(
    controller: NavController,
): DialogNavigator.Destination {
    val navigator = controller.navigatorProvider[DialogNavigator::class]
    return DialogNavigator.Destination(navigator) { dialogContent(controller) }.also {
        it.id = destinationId
    }
}

// the BottomSheet class and creator methods are marked with ExperimentalMaterialNavigationApi
// if those stay unused this method is never called, so we swallow the warning here
@OptIn(ExperimentalMaterialNavigationApi::class)
private fun BottomSheet.toDestination(
    controller: NavController,
): BottomSheetNavigator.Destination {
    val navigator = controller.navigatorProvider[BottomSheetNavigator::class]
    return BottomSheetNavigator.Destination(navigator) { bottomSheetContent(controller) }.also {
        it.id = destinationId
    }
}

private fun Activity.toDestination(
    controller: NavController,
): ActivityNavigator.Destination {
    val navigator = controller.navigatorProvider[ActivityNavigator::class]
    return ActivityNavigator.Destination(navigator).also {
        it.id = destinationId
        it.setIntent(intent)
    }
}
