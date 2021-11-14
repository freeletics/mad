package com.freeletics.mad.statemachine

import kotlinx.coroutines.flow.Flow

public fun interface FlowBuilder<InputState, T> {
    public fun build(state: Flow<InputState>): Flow<T>
}
