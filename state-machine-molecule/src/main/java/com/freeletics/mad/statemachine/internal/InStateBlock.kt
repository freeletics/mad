package com.freeletics.mad.statemachine.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.flow.Flow

internal interface InStateBlock<S : Any, A : Any> {
    @Composable
    fun DoThing(state: MutableState<S>, actions: Flow<A>)
}
