package com.freeletics.mad.statemachine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshotFlow
import com.freeletics.mad.statemachine.internal.InStateBlock
import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// TODO @DslMarker
@FlowPreview
@ExperimentalCoroutinesApi
public class InStateBuilderBlock<InputState : S, S : Any, A : Any>(
    private val _isInState: (S) -> Boolean
) {

    private val things = mutableListOf<InStateBlock<S, A>>()

    /**
     * Triggers every time an action of type [SubAction] is dispatched while the state machine is
     * in this state (as specified in the surrounding `in<State>` condition).
     *
     * An ongoing [handler] is cancelled when leaving this state or when a new [SubAction] is
     * dispatched.
     */
    public inline fun <reified SubAction : A> on(
        handler: OnActionHandler<InputState, S, SubAction>
    ) {
        on(ExecutionPolicy.CANCEL_PREVIOUS, handler)
    }

    /**
     * Triggers every time an action of type [SubAction] is dispatched while the state machine is
     * in this state.
     *
     * An ongoing [handler] is cancelled when leaving this state. [executionPolicy] is used to
     * determine the behavior when a new [SubAction] is dispatched while the previous [handler]
     * execution is still ongoing.
     */
    public inline fun <reified SubAction : A> on(
        executionPolicy: ExecutionPolicy,
        handler: OnActionHandler<InputState, S, SubAction>
    ) {
        on(SubAction::class, executionPolicy, handler)
    }

    /**
     * Triggers every time an action of type [SubAction] is dispatched while the state machine is
     * in this state.
     *
     * An ongoing [handler] is cancelled when leaving this state. [executionPolicy] is used to
     * determine the behavior when a new [SubAction] is dispatched while the previous [handler]
     * execution is still ongoing.
     */
    public fun <SubAction : A> on(
        actionClass: KClass<SubAction>,
        executionPolicy: ExecutionPolicy,
        handler: OnActionHandler<InputState, S, SubAction>
    ) {
        things += object : InStateBlock<S, A> {
            @Composable
            override fun DoThing(state: MutableState<S>, actions: Flow<A>) {
                LaunchedEffect(key1 = _isInState(state.value)) {
                    actions.filter { actionClass.isInstance(it) }
                        .flatMapWithExecutionPolicy(executionPolicy) {
                            flow {
                                val stateSnapshot = state.value
                                if (_isInState(stateSnapshot)) {
                                    @Suppress("unchecked_cast")
                                    emit(handler.handle(it as SubAction, stateSnapshot as InputState))
                                }
                            }
                        }
                        .collect {
                            it.reduce(state)
                        }
                }
            }
        }
    }

    /**
     *  An effect is a way to do some work without changing the state.
     *  A typical use case is to trigger navigation as some sort of side effect or
     *  triggering analytics or do logging.
     *  This is the "effect counterpart" to handling actions that you would do with [on].
     *  Thus, cancellation and so on works the same way as [on].
     *
     *  Per default it uses [ExecutionPolicy.CANCEL_PREVIOUS].
     */
    public inline fun <reified SubAction : A> onActionEffect(
        handler: OnActionEffectHandler<InputState, SubAction>
    ) {
        onActionEffect(
            executionPolicy = ExecutionPolicy.CANCEL_PREVIOUS,
            handler = handler
        )
    }

    /**
     *  An effect is a way to do some work without changing the state.
     *  A typical use case would be trigger navigation as some sort of side effect or
     *  triggering analytics.
     *  This is the "effect counterpart" to handling actions that you would do with [on].
     */
    public inline fun <reified SubAction : A> onActionEffect(
        executionPolicy: ExecutionPolicy,
        handler: OnActionEffectHandler<InputState, SubAction>
    ) {
        onActionEffect(SubAction::class, executionPolicy, handler)
    }

    /**
     *  An effect is a way to do some work without changing the state.
     *  A typical use case would be trigger navigation as some sort of side effect or
     *  triggering analytics.
     *  This is the "effect counterpart" to handling actions that you would do with [on].
     */
    public fun <SubAction : A> onActionEffect(
        actionClass: KClass<SubAction>,
        executionPolicy: ExecutionPolicy,
        handler: OnActionEffectHandler<InputState, SubAction>
    ) {
        on(
            actionClass = actionClass,
            executionPolicy = executionPolicy,
            handler = { action: SubAction, state: InputState ->
                handler.handle(action, state)
                NoStateChange
            }
        )
    }

    /**
     * Triggers every time the state machine enters this state.
     * It only triggers again if the surrounding `in<State>` condition is met and will only
     * re-trigger if `in<State>` condition returned false and then true again.
     *
     * An ongoing [handler] is cancelled when leaving this state.
     */
    public fun onEnter(
        handler: OnEnterHandler<InputState, S>
    ) {
        things += object : InStateBlock<S, A> {
            @Composable
            override fun DoThing(state: MutableState<S>, actions: Flow<A>) {
                LaunchedEffect(key1 = _isInState(state.value)) {
                    val stateSnapshot = state.value
                    if (_isInState(stateSnapshot)) {
                        @Suppress("unchecked_cast")
                        handler.handle(stateSnapshot as InputState).reduce(state)
                    }
                }
            }
        }
    }

    /**
     * An effect is a way to do some work without changing the state.
     * A typical use case is to trigger navigation as some sort of side effect or
     * triggering analytics or do logging.
     *
     * This is the "effect counterpart" of [onEnter] and follows the same logic when it triggers
     * and when it gets canceled.
     */
    public fun onEnterEffect(handler: OnEnterStateEffectHandler<InputState>) {
        onEnter { state ->
            handler.handle(state)
            NoStateChange
        }
    }

    /**
     * Triggers every time the state machine enters this state. The passed [flow] will be collected
     * and any emission will be passed to [handler].
     *
     * The collection as well as any ongoing [handler] is cancelled when leaving this state.
     *
     * [handler] will only be called for a new emission from [flow] after a previous [handler]
     * invocation completed.
     *
     * Per default [ExecutionPolicy.ORDERED] is applied.
     */
    public fun <T> collectWhileInState(
        flow: Flow<T>,
        handler: CollectFlowHandler<T, InputState, S>
    ) {
        collectWhileInState(flow, ExecutionPolicy.ORDERED, handler)
    }

    /**
     * Triggers every time the state machine enters this state. The passed [flow] will be collected
     * and any emission will be passed to [handler].
     *
     * The collection as well as any ongoing [handler] is cancelled when leaving this state.
     *
     * [executionPolicy] is used to determine the behavior when a new emission from [flow] arrives
     * before the previous [handler] invocation completed.
     */
    public fun <T> collectWhileInState(
        flow: Flow<T>,
        executionPolicy: ExecutionPolicy,
        handler: CollectFlowHandler<T, InputState, S>
    ) {
        things += object : InStateBlock<S, A> {
            @Composable
            override fun DoThing(state: MutableState<S>, actions: Flow<A>) {
                LaunchedEffect(key1 = _isInState(state.value)) {
                    flow.flatMapWithExecutionPolicy(executionPolicy) { value ->
                            flow {
                                val stateSnapshot = state.value
                                if (_isInState(stateSnapshot)) {
                                    @Suppress("unchecked_cast")
                                    emit(handler.handle(value, stateSnapshot as InputState))
                                }
                            }
                        }
                        .collect {
                            it.reduce(state)
                        }
                }
            }
        }
    }

    /**
     * Triggers every time the state machine enters this state. [flowBuilder] will get a
     * [Flow] that emits the current [InputState] and any change to it. The transformed `Flow` that
     * [flowBuilder] returns will be collected and any emission will be passed to [handler].
     *
     * The collection as well as any ongoing [handler] is cancelled when leaving this state.
     *
     * [handler] will only be called for a new emission from [flowBuilder]'s `Flow` after a
     * previous [handler] invocation completed.
     */
    public fun <T> collectWhileInState(
        flowBuilder: FlowBuilder<InputState, T>,
        handler: CollectFlowHandler<T, InputState, S>
    ) {
        collectWhileInState(flowBuilder, ExecutionPolicy.ORDERED, handler)
    }


    /**
     * Triggers every time the state machine enters this state. [flowBuilder] will get a
     * [Flow] that emits the current [InputState] and any change to it. The transformed `Flow` that
     * [flowBuilder] returns will be collected and any emission will be passed to [handler].
     *
     * The collection as well as any ongoing [handler] is cancelled when leaving this state.
     *
     * [executionPolicy] is used to determine the behavior when a new emission from [flowBuilder]'s
     * `Flow` arrives before the previous [handler] invocation completed.
     */
    public fun <T> collectWhileInState(
        flowBuilder: FlowBuilder<InputState, T>,
        executionPolicy: ExecutionPolicy,
        handler: CollectFlowHandler<T, InputState, S>
    ) {
        things += object : InStateBlock<S, A> {
            @Composable
            override fun DoThing(state: MutableState<S>, actions: Flow<A>) {
                LaunchedEffect(key1 = _isInState(state.value)) {
                    @Suppress("unchecked_cast")
                    flowBuilder.build(snapshotFlow { state.value as InputState })
                        .flatMapWithExecutionPolicy(executionPolicy) { value ->
                            flow {
                                val stateSnapshot = state.value
                                if (_isInState(stateSnapshot)) {
                                    @Suppress("unchecked_cast")
                                    emit(handler.handle(value, stateSnapshot as InputState))
                                }
                            }
                        }
                        .collect {
                            it.reduce(state)
                        }
                }
            }
        }
    }

    /**
     * An effect is a way to do some work without changing the state.
     * A typical use case is to trigger navigation as some sort of side effect or
     * triggering analytics or do logging.
     *
     * This is the "effect counterpart" of [collectWhileInState] and follows the same logic
     * when it triggers and when it gets canceled.
     *
     * Per default [ExecutionPolicy.ORDERED] is applied.
     */
    public fun <T> collectWhileInStateEffect(
        flow: Flow<T>,
        handler: CollectFlowEffectHandler<T, InputState>
    ) {
        collectWhileInStateEffect(
            flow = flow,
            executionPolicy = ExecutionPolicy.ORDERED,
            handler = handler
        )
    }

    /**
     * An effect is a way to do some work without changing the state.
     * A typical use case is to trigger navigation as some sort of side effect or
     * triggering analytics or do logging.
     *
     * This is the "effect counterpart" of [collectWhileInState] and follows the same logic
     * when it triggers and when it gets canceled.
     */
    public fun <T> collectWhileInStateEffect(
        flow: Flow<T>,
        executionPolicy: ExecutionPolicy,
        handler: CollectFlowEffectHandler<T, InputState>
    ) {
        collectWhileInState(
            flow = flow,
            executionPolicy = executionPolicy,
            handler = { value: T, state: InputState ->
                handler.handle(value, state)
                NoStateChange
            }
        )
    }

    /**
     * An effect is a way to do some work without changing the state.
     * A typical use case is to trigger navigation as some sort of side effect or
     * triggering analytics or do logging.
     *
     * This is the "effect counterpart" of [collectWhileInState] and follows the same logic
     * when it triggers and when it gets canceled.
     *
     * Per default [ExecutionPolicy.ORDERED] is applied.
     */
    public fun <T> collectWhileInStateEffect(
        flowBuilder: FlowBuilder<InputState, T>,
        handler: CollectFlowEffectHandler<T, InputState>
    ) {
        collectWhileInStateEffect(
            flowBuilder = flowBuilder,
            executionPolicy = ExecutionPolicy.ORDERED,
            handler = handler
        )
    }

    /**
     * An effect is a way to do some work without changing the state.
     * A typical use case is to trigger navigation as some sort of side effect or
     * triggering analytics or do logging.
     *
     * This is the "effect counterpart" of [collectWhileInState] and follows the same logic
     * when it triggers and when it gets canceled.
     */
    public fun <T> collectWhileInStateEffect(
        flowBuilder: FlowBuilder<InputState, T>,
        executionPolicy: ExecutionPolicy,
        handler: CollectFlowEffectHandler<T, InputState>
    ) {
        collectWhileInState(flowBuilder = flowBuilder,
            executionPolicy = executionPolicy,
            handler = { value: T, state: InputState ->
                handler.handle(value, state)
                NoStateChange
            })
    }

    public fun <SubStateMachineState : S> stateMachine(
        stateMachine: FlowReduxStateMachine<SubStateMachineState, A>,
        stateMapper: (InputState, SubStateMachineState) -> ChangeState<S> = { _, substateMachineState ->
            OverrideState(
                substateMachineState
            )
        }
    ) {
        stateMachine(
            stateMachine = stateMachine,
            actionMapper = { it },
            stateMapper = stateMapper
        )
    }

    public fun <SubStateMachineState : Any, SubStateMachineAction : Any> stateMachine(
        stateMachine: FlowReduxStateMachine<SubStateMachineState, SubStateMachineAction>,
        actionMapper: (A) -> SubStateMachineAction,
        stateMapper: (InputState, SubStateMachineState) -> ChangeState<S>
    ) {
        stateMachine(
            stateMachineFactory = { stateMachine },
            actionMapper = actionMapper,
            stateMapper = stateMapper
        )
    }


    public fun <SubStateMachineState : Any, SubStateMachineAction : Any> stateMachine(
        stateMachineFactory: (InputState) -> FlowReduxStateMachine<SubStateMachineState, SubStateMachineAction>,
        actionMapper: (A) -> SubStateMachineAction,
        stateMapper: (InputState, SubStateMachineState) -> ChangeState<S>
    ) {
        things += object : InStateBlock<S, A> {
            @Composable
            override fun DoThing(state: MutableState<S>, actions: Flow<A>) {
                LaunchedEffect(key1 = _isInState(state.value)) {
                    @Suppress("unchecked_cast")
                    val stateMachine = stateMachineFactory(state.value as InputState)

                    launch {
                        stateMachine.state
                            .map {
                                @Suppress("unchecked_cast")
                                stateMapper(state.value as InputState, it)
                            }
                            .collect {
                                it.reduce(state)
                            }
                    }

                    actions.collect { stateMachine.dispatch(actionMapper(it)) }
                }
            }
        }
    }

    internal fun build() = things.toList()
}
