package com.freeletics.mad.statemachine

import app.cash.molecule.moleculeFlow
import com.freeletics.mad.statemachine.StateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow

@FlowPreview
@ExperimentalCoroutinesApi
public abstract class FlowReduxStateMachine<S : Any, A : Any>(
    private val initialStateSupplier: () -> S,
) : StateMachine<S, A> {

    private val actions = Channel<A>(Channel.UNLIMITED)

    private lateinit var outputState: Flow<S>

    protected fun spec(specBlock: FlowReduxStoreBuilder<S, A>.() -> Unit) {
        if(!::outputState.isInitialized) {
            throw IllegalStateException(
                "State machine spec has already been set. " +
                    "It's only allowed to call spec {...} once."
            )
        }

        outputState = moleculeFlow {
            val builder = FlowReduxStoreBuilder<S, A>(initialStateSupplier, actions)
            specBlock(builder)
            builder.build()
        }
    }

    override val state: Flow<S>
        get() {
            checkSpecBlockSet()
            return outputState
        }

    override suspend fun dispatch(action: A) {
        checkSpecBlockSet()
        actions.send(action)
    }
    private fun checkSpecBlockSet() {
        if (!::outputState.isInitialized) {
            throw IllegalStateException(
                """
                    No state machine specs are defined. Did you call spec { ... } in init {...}?
                    Example usage:

                    class MyStateMachine : FlowReduxStateMachine<State, Action>(InitialState) {

                        init{
                            spec {
                                inState<FooState> {
                                    on<BarAction> { ... }
                                }
                                ...
                            }
                        }
                    }
                """.trimIndent()
            )
        }
    }
}
