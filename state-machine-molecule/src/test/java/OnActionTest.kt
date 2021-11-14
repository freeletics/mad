package com.freeletics.mad.statemachine

import app.cash.turbine.test
import kotlinx.coroutines.delay
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class, ExperimentalTime::class)
internal class OnActionTest {

    private val delay = 200L

    @Test
    fun `action block stops when moved to another state`() = suspendTest {
        var reached = false;
        var reachedBefore = false
        val sm = StateMachine {
            inState<TestState.Initial> {
                on<TestAction.A1> { _, _ ->
                    reachedBefore = true
                    delay(delay)
                    // this should never be reached because state transition did happen in the meantime,
                    // therefore this whole block must be canceled
                    reached = true
                    OverrideState(TestState.S1)
                }

                on<TestAction.A2> { _, _ ->
                    OverrideState(TestState.S2)
                }

            }

        }

        sm.state.test {
            assertEquals(TestState.Initial, awaitItem())
            sm.dispatchAsync(TestAction.A1)
            delay(delay/2)
            sm.dispatchAsync(TestAction.A2)
            assertEquals(TestState.S2, awaitItem())
            delay(delay)
            expectNoEvents()
        }

        assertTrue(reachedBefore)
        assertFalse(reached)
    }

    @Test
    fun `on action gets triggered and moves to next state`() = suspendTest {
        val sm = StateMachine {
            inState<TestState.Initial> {
                on<TestAction.A1> { _, _ ->
                    OverrideState(TestState.S1)
                }
            }

            inState<TestState.S1> {
                on<TestAction.A2> { _, _ ->
                    OverrideState(TestState.S2)
                }
            }
        }

        sm.state.test {
            assertEquals(TestState.Initial, awaitItem())
            sm.dispatchAsync(TestAction.A1)
            assertEquals(TestState.S1, awaitItem())
            sm.dispatchAsync(TestAction.A2)
            assertEquals(TestState.S2, awaitItem())
        }
    }
}
