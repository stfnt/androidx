/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("UNUSED_VARIABLE")
@MediumTest
@RunWith(AndroidJUnit4::class)
class SideEffectTests : BaseComposeTest() {

    @get:Rule
    override val activityRule = makeTestActivityRule()

    /**
     * Test that side effects run in order of appearance each time the composable
     * is recomposed.
     */
    @Test
    fun testSideEffectsRunInOrder() {
        val results = mutableListOf<Int>()
        var resultsAtComposition: List<Int>? = null
        var recompose: (() -> Unit)? = null
        compose {
            SideEffect {
                results += 1
            }
            SideEffect {
                results += 2
            }
            resultsAtComposition = results.toList()
            recompose = invalidate
        }.then {
            assertEquals(listOf(1, 2), results, "side effects were applied")
            assertEquals(
                emptyList(), resultsAtComposition,
                "side effects weren't applied until after composition"
            )
            recompose?.invoke() ?: error("missing recompose function")
        }.then {
            assertEquals(listOf(1, 2, 1, 2), results, "side effects applied a second time")
        }
    }

    /**
     * Test that side effects run after lifecycle observers enter the composition,
     * even if their remembrance happens after the SideEffect call appears.
     */
    @Test
    fun testSideEffectsRunAfterLifecycleObservers() {
        class MyObserver : CompositionLifecycleObserver {
            var isPresent: Boolean = false
                private set

            override fun onEnter() {
                isPresent = true
            }

            override fun onLeave() {
                isPresent = false
            }
        }

        val myObserverOne = MyObserver()
        val myObserverTwo = MyObserver()
        var wasObserverOnePresent = false
        var wasObserverTwoPresent = false

        compose {
            val one = remember { myObserverOne }
            SideEffect {
                wasObserverOnePresent = myObserverOne.isPresent
                wasObserverTwoPresent = myObserverTwo.isPresent
            }
            val two = remember { myObserverTwo }
        }.then {
            assertTrue(wasObserverOnePresent, "observer one present for side effect")
            assertTrue(wasObserverTwoPresent, "observer two present for side effect")
        }
    }

    @Test
    fun testDisposableEffectExecutionOrder() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            log("Unmountable:start")
            DisposableEffect(Unit) {
                log("DisposableEffect")
                onDispose {
                    log("onDispose")
                }
            }
            log("Unmountable:end")
        }

        compose {
            log("compose:start")
            if (mount) {
                Unmountable()
            }
            log("compose:end")
        }.then { _ ->
            assertEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "DisposableEffect"
                ),
                logHistory
            )
            mount = false
        }.then { _ ->
            assertEquals(
                listOf(
                    "compose:start",
                    "Unmountable:start",
                    "Unmountable:end",
                    "compose:end",
                    "DisposableEffect",
                    "compose:start",
                    "compose:end",
                    "onDispose"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testDisposableEffectRelativeOrdering() {
        var mount by mutableStateOf(true)

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        @Composable
        fun Unmountable() {
            DisposableEffect(Unit) {
                log("DisposableEffect:a2")
                onDispose {
                    log("onDispose:a2")
                }
            }
            DisposableEffect(Unit) {
                log("DisposableEffect:b2")
                onDispose {
                    log("onDispose:b2")
                }
            }
        }

        compose {
            DisposableEffect(NeverEqualObject) {
                log("DisposableEffect:a1")
                onDispose {
                    log("onDispose:a1")
                }
            }
            if (mount) {
                Unmountable()
            }
            DisposableEffect(NeverEqualObject) {
                log("DisposableEffect:b1")
                onDispose {
                    log("onDispose:b1")
                }
            }
        }.then { _ ->
            assertEquals(
                listOf(
                    "DisposableEffect:a1",
                    "DisposableEffect:a2",
                    "DisposableEffect:b2",
                    "DisposableEffect:b1"
                ),
                logHistory
            )
            mount = false
            log("recompose")
        }.then { _ ->
            assertEquals(
                listOf(
                    "DisposableEffect:a1",
                    "DisposableEffect:a2",
                    "DisposableEffect:b2",
                    "DisposableEffect:b1",
                    "recompose",
                    "onDispose:b2",
                    "onDispose:a2",
                    "onDispose:b1",
                    "onDispose:a1",
                    "DisposableEffect:a1",
                    "DisposableEffect:b1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testDisposableEffectKeyChange() {
        var x = 0
        var key = 123
        lateinit var recompose: () -> Unit

        val logHistory = mutableListOf<String>()
        fun log(x: String) = logHistory.add(x)

        compose {
            recompose = invalidate
            DisposableEffect(key) {
                val y = x++
                log("DisposableEffect:$y")
                onDispose {
                    log("dispose:$y")
                }
            }
        }.then { _ ->
            log("recompose")
            recompose()
        }.then { _ ->
            assertEquals(
                listOf(
                    "DisposableEffect:0",
                    "recompose"
                ),
                logHistory
            )
            log("recompose (key -> 345)")
            key = 345
            recompose()
        }.then { _ ->
            assertEquals(
                listOf(
                    "DisposableEffect:0",
                    "recompose",
                    "recompose (key -> 345)",
                    "dispose:0",
                    "DisposableEffect:1"
                ),
                logHistory
            )
        }
    }

    @Test
    fun testRememberUpdatedStateRecomposition() {
        @Composable
        fun MyComposable(
            arg: String,
            inCh: ReceiveChannel<Unit>,
            outCh: SendChannel<String>
        ) {
            val currentArg by rememberUpdatedState(arg)

            // This block closes over currentArg and is long-lived; it is important that the
            // value used be updated by recomposition of MyComposable
            LaunchedTask {
                inCh.receive()
                outCh.send(currentArg)
            }
        }

        var myComposableArg by mutableStateOf("hello")
        val pleaseSend = Channel<Unit>()
        val output = Channel<String>()

        compose {
            MyComposable(myComposableArg, pleaseSend, output)
        }.then {
            myComposableArg = "world"
        }.then {
            val offerSucceeded = pleaseSend.offer(Unit)
            assertTrue(offerSucceeded, "task wasn't awaiting send signal")
        }.then {
            val receivedResult = output.poll()
            assertEquals("world", receivedResult)
        }
    }

    /**
     * Always compares as unequal to itself (and everything else) to force DisposableEffect
     * to recompose on every recomposition
     */
    @Suppress("EqualsOrHashCode")
    private object NeverEqualObject {
        override fun equals(other: Any?) = false
        override fun hashCode() = 42
    }
}
