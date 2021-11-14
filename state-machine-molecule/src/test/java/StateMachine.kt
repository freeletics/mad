package com.freeletics.mad.statemachine

import com.freeletics.mad.statemachine.FlowReduxStateMachine
import com.freeletics.mad.statemachine.FlowReduxStoreBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Test
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
internal class StateMachine constructor(
    initialState : TestState = TestState.Initial,
    specBlock: FlowReduxStoreBuilder<TestState, TestAction>.() -> Unit
) : FlowReduxStateMachine<TestState, TestAction>(
    initialStateSupplier = { initialState }
) {

    init {
        spec(specBlock)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun dispatchAsync(action: TestAction) {
        GlobalScope.launch {
            dispatch(action)
        }
    }
}
