package com.freeletics.mad.statemachine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.freeletics.mad.statemachine.internal.InStateBlock
import kotlin.jvm.JvmSynthetic
import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn

@FlowPreview
@ExperimentalCoroutinesApi
public class FlowReduxStoreBuilder<S : Any, A : Any>(
    private val initialStateSupplier: () -> S,
    private val actions: Channel<A>
) {

    private val inStateBlocks = mutableListOf<InStateBlock<S, A>>()

    /**
     * Define what happens if the store is in a certain state.
     * "In a certain state" condition is true if state is instance of the type specified as generic function parameter.
     */
    public inline fun <reified SubState : S> inState(
        noinline block: InStateBuilderBlock<SubState, S, A>.() -> Unit
    ) {
        inState(SubState::class, block)
    }

    /**
     * Define what happens if the store is in a certain state.
     * "In a certain state" condition is true if state is instance of the type specified as generic function parameter.
     */
    @JvmSynthetic
    public fun <SubState : S> inState(
        subStateClass: KClass<SubState>,
        block: InStateBuilderBlock<SubState, S, A>.() -> Unit
    ) {
        val builder = InStateBuilderBlock<SubState, S, A> {
            subStateClass.isInstance(it)
        }
        block(builder)
        inStateBlocks += builder.build()
    }

    /**
     * This variation allows you to specify is a mix between inferring the condition of the generic function type
     * and additionally can specify and ADDITIONAL condition that also must be true in addition to the check that
     * the type as specified as generic fun parameter is an instance of the current state.
     */
    public inline fun <reified SubState : S> inState(
        noinline additionalIsInState: (SubState) -> Boolean,
        noinline block: InStateBuilderBlock<SubState, S, A>.() -> Unit
    ) {
        inState(SubState::class, additionalIsInState, block)
    }

    /**
     * This variation allows you to specify is a mix between inferring the condition of the generic function type
     * and additionally can specify and ADDITIONAL condition that also must be true in addition to the check that
     * the type as specified as generic fun parameter is an instance of the current state.
     */
    public fun <SubState : S> inState(
        subStateClass: KClass<SubState>,
        additionalIsInState: (SubState) -> Boolean,
        block: InStateBuilderBlock<SubState, S, A>.() -> Unit
    ) {
        val builder = InStateBuilderBlock<SubState, S, A> {
            @Suppress("UNCHECKED_CAST")
            subStateClass.isInstance(it) && additionalIsInState(it as SubState)
        }
        block(builder)
        inStateBlocks += builder.build()
    }

    /**
     * Define what happens if the store is in a certain state.
     * @param isInState The condition under which we identify that the state machine is in a given "state".
     */
    public fun inStateWithCondition(
        isInState: (S) -> Boolean,
        block: InStateBuilderBlock<S, S, A>.() -> Unit
    ) {
        val builder = InStateBuilderBlock<S, S, A> {
            isInState(it)
        }
        block(builder)
        inStateBlocks += builder.build()
    }

    @Composable
    internal fun build(): S {
        val state = remember { mutableStateOf(initialStateSupplier()) }

        val scope = rememberCoroutineScope()
        val actions = this.actions.receiveAsFlow().shareIn(scope, SharingStarted.WhileSubscribed())

        inStateBlocks.forEach {
            it.DoThing(state = state, actions = actions)
        }

        return state.value
    }
}
