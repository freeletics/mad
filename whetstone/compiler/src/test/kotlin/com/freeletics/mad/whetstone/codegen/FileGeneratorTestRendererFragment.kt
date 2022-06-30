package com.freeletics.mad.whetstone.codegen

import com.freeletics.mad.whetstone.Navigation
import com.freeletics.mad.whetstone.RendererFragmentData
import com.squareup.kotlinpoet.ClassName
import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class FileGeneratorTestRendererFragment {

    private val navigation = Navigation.Fragment(
        route = ClassName("com.test", "TestRoute"),
        destinationType = "NONE",
        destinationScope = ClassName("com.test.destination", "TestDestinationScope"),
        navEntryData = null,
    )

    private val data = RendererFragmentData(
        baseName = "Test",
        packageName = "com.test",
        scope = ClassName("com.test", "TestScreen"),
        parentScope = ClassName("com.test.parent", "TestParentScope"),
        stateMachine = ClassName("com.test", "TestStateMachine"),
        factory = ClassName("com.test", "RendererFactory"),
        fragmentBaseClass = ClassName("androidx.fragment.app", "Fragment"),
        navigation = null,
    )

    @Test
    fun `generates code for RendererFragmentData`() {
        val actual = FileGenerator().generate(data).toString()

        val expected = """
            package com.test

            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import androidx.fragment.app.Fragment
            import androidx.lifecycle.SavedStateHandle
            import androidx.lifecycle.ViewModel
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.fragment.`internal`.viewModel
            import com.gabrielittner.renderer.connect.connect
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.Unit
            import kotlin.collections.Set

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface RetainedTestComponent {
              public val testStateMachine: TestStateMachine
            
              public val closeables: Set<Closeable>

              public val rendererFactory: RendererFactory

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    arguments: Bundle): RetainedTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun retainedTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface RetainedTestModule {
              @Multibinds
              public fun bindCancellable(): Set<Closeable>
            }

            @InternalWhetstoneApi
            internal class TestViewModel(
              parentComponent: RetainedTestComponent.ParentComponent,
              savedStateHandle: SavedStateHandle,
              arguments: Bundle,
            ) : ViewModel() {
              public val component: RetainedTestComponent =
                  parentComponent.retainedTestComponentFactory().create(savedStateHandle, arguments)

              public override fun onCleared(): Unit {
                component.closeables.forEach {
                  it.close()
                }
              }
            }
            
            @OptIn(InternalWhetstoneApi::class)
            public class TestFragment : Fragment() {
              private lateinit var retainedTestComponent: RetainedTestComponent

              public override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?,
              ): View {
                if (!::retainedTestComponent.isInitialized) {
                  val arguments = requireArguments()
                  val viewModel = viewModel(TestParentScope::class, arguments, ::TestViewModel)
                  retainedTestComponent = viewModel.component
                }
            
                val renderer = retainedTestComponent.rendererFactory.inflate(inflater, container)
                connect(renderer, retainedTestComponent.testStateMachine)
                return renderer.rootView
              }
            }
            
        """.trimIndent()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `generates code for RendererFragmentData with navigation`() {
        val withNavigation = data.copy(navigation = navigation)
        val actual = FileGenerator().generate(withNavigation).toString()

        val expected = """
            package com.test

            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import androidx.fragment.app.Fragment
            import androidx.lifecycle.SavedStateHandle
            import androidx.lifecycle.ViewModel
            import com.freeletics.mad.navigator.NavEventNavigator
            import com.freeletics.mad.navigator.fragment.handleNavigation
            import com.freeletics.mad.navigator.fragment.requireRoute
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.DestinationComponent
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.fragment.`internal`.viewModel
            import com.gabrielittner.renderer.connect.connect
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.destination.TestDestinationScope
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.Unit
            import kotlin.collections.Set

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface RetainedTestComponent {
              public val testStateMachine: TestStateMachine

              public val navEventNavigator: NavEventNavigator
            
              public val closeables: Set<Closeable>

              public val rendererFactory: RendererFactory

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    testRoute: TestRoute): RetainedTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun retainedTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface RetainedTestModule {
              @Multibinds
              public fun bindCancellable(): Set<Closeable>
            }

            @InternalWhetstoneApi
            internal class TestViewModel(
              parentComponent: RetainedTestComponent.ParentComponent,
              savedStateHandle: SavedStateHandle,
              testRoute: TestRoute,
            ) : ViewModel() {
              public val component: RetainedTestComponent =
                  parentComponent.retainedTestComponentFactory().create(savedStateHandle, testRoute)

              public override fun onCleared(): Unit {
                component.closeables.forEach {
                  it.close()
                }
              }
            }
            
            @OptIn(InternalWhetstoneApi::class)
            public class TestFragment : Fragment() {
              private lateinit var retainedTestComponent: RetainedTestComponent

              public override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?,
              ): View {
                if (!::retainedTestComponent.isInitialized) {
                  val testRoute = requireRoute<TestRoute>()
                  val viewModel = viewModel(TestParentScope::class, TestDestinationScope::class, testRoute,
                      ::TestViewModel)
                  retainedTestComponent = viewModel.component
            
                  handleNavigation(this, retainedTestComponent.navEventNavigator)
                }
            
                val renderer = retainedTestComponent.rendererFactory.inflate(inflater, container)
                connect(renderer, retainedTestComponent.testStateMachine)
                return renderer.rootView
              }
            }
            
            @ContributesTo(TestDestinationScope::class)
            @OptIn(InternalWhetstoneApi::class)
            public interface NavEntryTestDestinationComponent : DestinationComponent
            
        """.trimIndent()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `generates code for RendererFragmentData with navigation and destination`() {
        val withDestination = data.copy(navigation = navigation.copy(destinationType = "SCREEN"))
        val actual = FileGenerator().generate(withDestination).toString()

        val expected = """
            package com.test

            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import androidx.fragment.app.Fragment
            import androidx.lifecycle.SavedStateHandle
            import androidx.lifecycle.ViewModel
            import com.freeletics.mad.navigator.NavEventNavigator
            import com.freeletics.mad.navigator.fragment.NavDestination
            import com.freeletics.mad.navigator.fragment.ScreenDestination
            import com.freeletics.mad.navigator.fragment.handleNavigation
            import com.freeletics.mad.navigator.fragment.requireRoute
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.DestinationComponent
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.fragment.`internal`.viewModel
            import com.gabrielittner.renderer.connect.connect
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.destination.TestDestinationScope
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.Provides
            import dagger.multibindings.IntoSet
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.Unit
            import kotlin.collections.Set

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface RetainedTestComponent {
              public val testStateMachine: TestStateMachine

              public val navEventNavigator: NavEventNavigator
            
              public val closeables: Set<Closeable>

              public val rendererFactory: RendererFactory

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    testRoute: TestRoute): RetainedTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun retainedTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface RetainedTestModule {
              @Multibinds
              public fun bindCancellable(): Set<Closeable>
            }

            @InternalWhetstoneApi
            internal class TestViewModel(
              parentComponent: RetainedTestComponent.ParentComponent,
              savedStateHandle: SavedStateHandle,
              testRoute: TestRoute,
            ) : ViewModel() {
              public val component: RetainedTestComponent =
                  parentComponent.retainedTestComponentFactory().create(savedStateHandle, testRoute)

              public override fun onCleared(): Unit {
                component.closeables.forEach {
                  it.close()
                }
              }
            }
            
            @OptIn(InternalWhetstoneApi::class)
            public class TestFragment : Fragment() {
              private lateinit var retainedTestComponent: RetainedTestComponent

              public override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?,
              ): View {
                if (!::retainedTestComponent.isInitialized) {
                  val testRoute = requireRoute<TestRoute>()
                  val viewModel = viewModel(TestParentScope::class, TestDestinationScope::class, testRoute,
                      ::TestViewModel)
                  retainedTestComponent = viewModel.component
            
                  handleNavigation(this, retainedTestComponent.navEventNavigator)
                }
            
                val renderer = retainedTestComponent.rendererFactory.inflate(inflater, container)
                connect(renderer, retainedTestComponent.testStateMachine)
                return renderer.rootView
              }
            }
            
            @ContributesTo(TestDestinationScope::class)
            @OptIn(InternalWhetstoneApi::class)
            public interface NavEntryTestDestinationComponent : DestinationComponent
            
            @Module
            @ContributesTo(TestDestinationScope::class)
            public object WhetstoneTestNavDestinationModule {
              @Provides
              @IntoSet
              public fun provideNavDestination(): NavDestination = ScreenDestination<TestRoute, TestFragment>()
            }
            
        """.trimIndent()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `generates code for RendererFragmentData, dialog fragment`() {
        val dialogFragment = data.copy(
            fragmentBaseClass = ClassName("androidx.fragment.app", "DialogFragment")
        )
        val actual = FileGenerator().generate(dialogFragment).toString()

        val expected = """
            package com.test

            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import androidx.fragment.app.DialogFragment
            import androidx.lifecycle.SavedStateHandle
            import androidx.lifecycle.ViewModel
            import com.freeletics.mad.whetstone.ScopeTo
            import com.freeletics.mad.whetstone.`internal`.InternalWhetstoneApi
            import com.freeletics.mad.whetstone.fragment.`internal`.viewModel
            import com.gabrielittner.renderer.connect.connect
            import com.squareup.anvil.annotations.ContributesSubcomponent
            import com.squareup.anvil.annotations.ContributesTo
            import com.test.parent.TestParentScope
            import dagger.BindsInstance
            import dagger.Module
            import dagger.multibindings.Multibinds
            import java.io.Closeable
            import kotlin.OptIn
            import kotlin.Unit
            import kotlin.collections.Set

            @OptIn(InternalWhetstoneApi::class)
            @ScopeTo(TestScreen::class)
            @ContributesSubcomponent(
              scope = TestScreen::class,
              parentScope = TestParentScope::class,
            )
            public interface RetainedTestComponent {
              public val testStateMachine: TestStateMachine
            
              public val closeables: Set<Closeable>

              public val rendererFactory: RendererFactory

              @ContributesSubcomponent.Factory
              public interface Factory {
                public fun create(@BindsInstance savedStateHandle: SavedStateHandle, @BindsInstance
                    arguments: Bundle): RetainedTestComponent
              }

              @ContributesTo(TestParentScope::class)
              public interface ParentComponent {
                public fun retainedTestComponentFactory(): Factory
              }
            }

            @Module
            @ContributesTo(TestScreen::class)
            public interface RetainedTestModule {
              @Multibinds
              public fun bindCancellable(): Set<Closeable>
            }

            @InternalWhetstoneApi
            internal class TestViewModel(
              parentComponent: RetainedTestComponent.ParentComponent,
              savedStateHandle: SavedStateHandle,
              arguments: Bundle,
            ) : ViewModel() {
              public val component: RetainedTestComponent =
                  parentComponent.retainedTestComponentFactory().create(savedStateHandle, arguments)

              public override fun onCleared(): Unit {
                component.closeables.forEach {
                  it.close()
                }
              }
            }
            
            @OptIn(InternalWhetstoneApi::class)
            public class TestFragment : DialogFragment() {
              private lateinit var retainedTestComponent: RetainedTestComponent

              public override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?,
              ): View {
                if (!::retainedTestComponent.isInitialized) {
                  val arguments = requireArguments()
                  val viewModel = viewModel(TestParentScope::class, arguments, ::TestViewModel)
                  retainedTestComponent = viewModel.component
                }
            
                val renderer = retainedTestComponent.rendererFactory.inflate(inflater, container)
                connect(renderer, retainedTestComponent.testStateMachine)
                return renderer.rootView
              }
            }

        """.trimIndent()

        assertThat(actual).isEqualTo(expected)
    }
}
