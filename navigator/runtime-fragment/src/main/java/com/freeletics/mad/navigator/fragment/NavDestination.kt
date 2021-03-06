package com.freeletics.mad.navigator.fragment

import android.content.Intent
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.freeletics.mad.navigator.ActivityRoute
import com.freeletics.mad.navigator.BaseRoute
import com.freeletics.mad.navigator.NavRoute
import com.freeletics.mad.navigator.fragment.NavDestination.Activity
import com.freeletics.mad.navigator.fragment.NavDestination.Dialog
import com.freeletics.mad.navigator.fragment.NavDestination.Screen
import kotlin.reflect.KClass

/**
 * Creates a new [NavDestination] that represents a full screen. The class of [T] will be used
 * as a unique identifier. The given `Fragment` class  [F] will be shown when the screen is being
 * navigated to using an instance of [T].
 */
@Suppress("FunctionName")
public inline fun <reified T : BaseRoute, reified F : Fragment> ScreenDestination():
    NavDestination = Screen(T::class, F::class)

/**
 * Creates a new [NavDestination] that represents a dialog. The class of [T] will be used
 * as a unique identifier. The given `Fragment` class  [F] will be shown when the screen is being
 * navigated to using an instance of [T].
 */
@Suppress("FunctionName")
public inline fun <reified T : NavRoute, reified F : DialogFragment> DialogDestination():
    NavDestination = Dialog(T::class, F::class)

/**
 * Creates a new [NavDestination] that represents an `Activity`. The class of [T] will be used
 * as a unique identifier. The given [intent] will be used to launch the `Activity` when using an
 * instance of [T] for navigation.
 */
@Suppress("FunctionName")
public inline fun <reified T : ActivityRoute> ActivityDestination(
    intent: Intent,
): NavDestination = Activity(T::class, intent)

/**
 * A destination that can be navigated to. See [setGraph] for how to configure a `NavGraph` with it.
 */
public sealed interface NavDestination {
    /**
     * Represents a full screen. The [route] will be used as a unique identifier. The given
     * [fragmentClass] will be shown when the screen is being
     * navigated to using an instance of [route].
     */
    public class Screen<T : BaseRoute>(
        internal val route: KClass<T>,
        internal val fragmentClass: KClass<out Fragment>,
    ) : NavDestination

    /**
     * Represents a dialog. The [route] will be used as a unique identifier. The given
     * [fragmentClass] will be shown when it's being navigated to using an instance of [route].
     */
    public class Dialog<T : NavRoute>(
        internal val route: KClass<T>,
        internal val fragmentClass: KClass<out DialogFragment>,
    ) : NavDestination

    /**
     * Represents an `Activity`. The [route] will be used as a unique identifier. The given
     * [intent] will be used to launch the `Activity` when using an instance of [route] for
     * navigation.
     */
    public class Activity<T : ActivityRoute>(
        internal val route: KClass<T>,
        internal val intent: Intent,
    ) : NavDestination
}
