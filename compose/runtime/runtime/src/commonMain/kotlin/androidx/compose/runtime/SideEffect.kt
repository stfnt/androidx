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

/**
 * Schedule [effect] to run when the current composition completes successfully and applies
 * changes. [SideEffect] can be used to apply side effects to objects managed by the
 * composition that are not backed by [snapshots][androidx.compose.runtime.snapshots.Snapshot] so
 * as not to leave those objects in an inconsistent state if the current composition operation
 * fails.
 *
 * [effect] will always be run on the composition's apply dispatcher and appliers are never run
 * concurrent with themselves, one another, applying changes to the composition tree, or running
 * [CompositionLifecycleObserver] event callbacks. [SideEffect]s are always run after
 * [CompositionLifecycleObserver] event callbacks.
 *
 * A [SideEffect] runs after **every** recomposition. To launch an ongoing task spanning
 * potentially many recompositions, see [LaunchedTask]. To manage an event subscription or other
 * object lifecycle, see [DisposableEffect].
 */
@Composable
@ComposableContract(restartable = false)
fun SideEffect(
    effect: () -> Unit
) {
    currentComposer.recordSideEffect(effect)
}

/**
 * Receiver scope for [DisposableEffect] that offers the [onDispose] clause that should be
 * the last statement in any call to [DisposableEffect].
 */
class DisposableEffectScope {
    /**
     * Provide [onDisposeEffect] to the [DisposableEffect] to run when it leaves the composition
     * or its subject changes.
     */
    inline fun onDispose(
        crossinline onDisposeEffect: () -> Unit
    ): DisposableEffectDisposable = object : DisposableEffectDisposable {
        override fun dispose() {
            onDisposeEffect()
        }
    }
}

interface DisposableEffectDisposable {
    fun dispose()
}

private val InternalDisposableEffectScope = DisposableEffectScope()

private class DisposableEffectImpl(
    private val effect: DisposableEffectScope.() -> DisposableEffectDisposable
) : CompositionLifecycleObserver {
    private var onDispose: DisposableEffectDisposable? = null

    override fun onEnter() {
        onDispose = InternalDisposableEffectScope.effect()
    }

    override fun onLeave() {
        onDispose?.dispose()
        onDispose = null
    }
}

private const val DisposableEffectNoParamError =
    "DisposableEffect must provide one or more 'subject' parameters that define the identity of " +
        "the DisposableEffect and determine when its previous effect should be disposed and " +
        "restart."

@Composable
@ComposableContract(restartable = false)
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Deprecated(DisposableEffectNoParamError, level = DeprecationLevel.ERROR)
fun DisposableEffect(
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
): Unit = error(DisposableEffectNoParamError)

/**
 * A side effect of composition that must run for any new unique value of [subject] and must be
 * reversed or cleaned up if [subject] changes or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _subject_ is a value that defines the identity of the
 * [DisposableEffect]. If a subject changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of subjects include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a subject and reinitialize
 * when a different subject is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedTask] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun DisposableEffect(
    subject: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(subject) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [subject1] or [subject2]
 * and must be reversed or cleaned up if [subject1] or [subject2] changes, or if the
 * [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _subject_ is a value that defines the identity of the
 * [DisposableEffect]. If a subject changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of subjects include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a subject and reinitialize
 * when a different subject is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedTask] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun DisposableEffect(
    subject1: Any?,
    subject2: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(subject1, subject2) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [subject1], [subject2]
 * or [subject3] and must be reversed or cleaned up if [subject1], [subject2] or [subject3]
 * changes, or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _subject_ is a value that defines the identity of the
 * [DisposableEffect]. If a subject changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of subjects include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a subject and reinitialize
 * when a different subject is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedTask] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun DisposableEffect(
    subject1: Any?,
    subject2: Any?,
    subject3: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(subject1, subject2, subject3) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [subjects] and must
 * be reversed or cleaned up if any [subjects] change or if the [DisposableEffect] leaves the
 * composition.
 *
 * A [DisposableEffect]'s _subject_ is a value that defines the identity of the
 * [DisposableEffect]. If a subject changes, the [DisposableEffect] must
 * [dispose][DisposableEffectScope.onDispose] its current [effect] and reset by calling [effect]
 * again. Examples of subjects include:
 *
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a subject and reinitialize
 * when a different subject is provided, performing cleanup for the old operation before
 * initializing the new. For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause
 * as the final statement in its [effect] block. If your operation does not require disposal
 * it might be a [SideEffect] instead, or a [LaunchedTask] if it launches a coroutine that should
 * be managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run
 * on the composition's apply dispatcher and appliers are never run concurrent with themselves,
 * one another, applying changes to the composition tree, or running [CompositionLifecycleObserver]
 * event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
@Suppress("ArrayReturn")
fun DisposableEffect(
    vararg subjects: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectDisposable
) {
    remember(*subjects) { DisposableEffectImpl(effect) }
}
