package com.freeletics.mad.navigator.compose

import android.app.Activity as AndroidActivity
import androidx.annotation.IdRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.ActivityNavigator
import androidx.navigation.NavController
import androidx.navigation.NavDestination as AndroidxNavDestination
import androidx.navigation.compose.NavHost as AndroidXNavHost
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavArgument
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
import com.freeletics.mad.navigator.internal.ObsoleteNavigatorApi
import com.freeletics.mad.navigator.internal.toNavRoot
import com.freeletics.mad.navigator.internal.toNavRoute
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator

/**
 * Create a new [androidx.navigation.compose.NavHost] with a [androidx.navigation.NavGraph]
 * containing all given [destinations]. [startRoot] will be used as the start destination
 * of the graph.
 */
@ExperimentalMaterialNavigationApi
@Composable
public fun NavHost(
    startRoot: NavRoot,
    destinations: Set<NavDestination>,
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val startDestinationId = startRoot.destinationId
    ModalBottomSheetLayout(bottomSheetNavigator) {
        NavHost(navController, startDestinationId, destinations)
    }
}

/**
 * Create a new [androidx.navigation.compose.NavHost] with a [androidx.navigation.NavGraph]
 * containing all given [destinations]. [startRoute] will be used as the start destination
 * of the graph.
 */
@ExperimentalMaterialNavigationApi
@Composable
public fun NavHost(
    startRoute: NavRoute,
    destinations: Set<NavDestination>,
) {
    val bottomSheetNavigator = rememberBottomSheetNavigator()
    val navController = rememberNavController(bottomSheetNavigator)
    val startDestinationId = startRoute.destinationId
    ModalBottomSheetLayout(bottomSheetNavigator) {
        NavHost(navController, startDestinationId, destinations)
    }
}

@Composable
private fun NavHost(
    navController: NavHostController,
    @IdRes startDestinationId: Int,
    destinations: Set<NavDestination>,
) {
    val graph = remember(navController, startDestinationId, destinations) {
        @Suppress("deprecation")
        navController.createGraph(startDestination = startDestinationId) {
            destinations.forEach { destination ->
                addDestination(navController, destination)
            }
        }
    }

    LegacyFindNavControllerSupport(navController)

    CompositionLocalProvider(LocalNavController provides navController) {
        AndroidXNavHost(navController, graph)
    }
}

// the BottomSheet class and creator methods are marked with ExperimentalMaterialNavigationApi
// if those stay unused the experimental code is never called, so we swallow the warning here
@OptIn(ExperimentalMaterialNavigationApi::class)
private fun NavGraphBuilder.addDestination(
    controller: NavController,
    destination: NavDestination,
) {
    val newDestination = when (destination) {
        is Screen<*> -> destination.toDestination(controller)
        is RootScreen<*> -> destination.toDestination(controller)
        is Dialog<*> -> destination.toDestination(controller)
        is BottomSheet<*> -> destination.toDestination(controller)
        is Activity -> destination.toDestination(controller)
    }
    addDestination(newDestination)
}

private fun <T : NavRoute> Screen<T>.toDestination(
    controller: NavController,
): ComposeNavigator.Destination {
    val navigator = controller.navigatorProvider[ComposeNavigator::class]
    return ComposeNavigator.Destination(navigator) { screenContent(it.arguments!!.toNavRoute()) }.also {
        it.id = destinationId
        it.addDefaultArguments(defaultArguments)
    }
}

private fun <T : NavRoot> RootScreen<T>.toDestination(
    controller: NavController,
): ComposeNavigator.Destination {
    val navigator = controller.navigatorProvider[ComposeNavigator::class]
    return ComposeNavigator.Destination(navigator) { screenContent(it.arguments!!.toNavRoot()) }.also {
        it.id = destinationId
        it.addDefaultArguments(defaultArguments)
    }
}

private fun <T : NavRoute> Dialog<T>.toDestination(
    controller: NavController,
): DialogNavigator.Destination {
    val navigator = controller.navigatorProvider[DialogNavigator::class]
    return DialogNavigator.Destination(navigator) { dialogContent(it.arguments!!.toNavRoute()) }.also {
        it.id = destinationId
        it.addDefaultArguments(defaultArguments)
    }
}

// the BottomSheet class and creator methods are marked with ExperimentalMaterialNavigationApi
// if those stay unused this method is never called, so we swallow the warning here
@OptIn(ExperimentalMaterialNavigationApi::class)
private fun <T : NavRoute> BottomSheet<T>.toDestination(
    controller: NavController,
): BottomSheetNavigator.Destination {
    val navigator = controller.navigatorProvider[BottomSheetNavigator::class]
    return BottomSheetNavigator.Destination(navigator) { bottomSheetContent(it.arguments!!.toNavRoute()) }.also {
        it.id = destinationId
        it.addDefaultArguments(defaultArguments)
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

internal val LocalNavController = staticCompositionLocalOf<NavController> {
    throw IllegalStateException("Can't use NavEventNavigationHandler outside of a navigator NavHost")
}

private fun AndroidxNavDestination.addDefaultArguments(extras: Bundle?) {
    extras?.keySet()?.forEach { key ->
        val argument = NavArgument.Builder()
            .setDefaultValue(extras.get(key))
            .setIsNullable(false)
            .build()
        addArgument(key, argument)
    }
}

@ObsoleteNavigatorApi
public fun AndroidActivity.findComposeNavController(): NavController? {
    val view = findViewById<View>(android.R.id.content)!!
    return view.getTag(navControllerTagId) as NavController?
}

@Composable
private fun LegacyFindNavControllerSupport(navController: NavController) {
    val context = LocalContext.current
    DisposableEffect(navController, context) {
        val view = context.findActivity().findViewById<View>(android.R.id.content)!!
        view.setTag(navControllerTagId, navController)
        onDispose {
            view.setTag(navControllerTagId, null)
        }
    }
}

private fun Context.findActivity(): android.app.Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is android.app.Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}

private val navControllerTagId = View.generateViewId()