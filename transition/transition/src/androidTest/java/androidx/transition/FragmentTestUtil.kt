/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.transition

import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.TargetTracking
import androidx.transition.test.R
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.testutils.runOnUiThreadRethrow
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.ref.WeakReference
import java.util.ArrayList

fun ActivityTestRule<out FragmentActivity>.executePendingTransactions(
    fm: FragmentManager = activity.supportFragmentManager
): Boolean {
    var ret = false
    runOnUiThreadRethrow { ret = fm.executePendingTransactions() }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.popBackStackImmediate(): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var ret = false
    instrumentation.runOnMainSync {
        ret = activity.supportFragmentManager.popBackStackImmediate()
    }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.popBackStackImmediate(
    id: Int,
    flags: Int = 0
): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var ret = false
    instrumentation.runOnMainSync {
        ret = activity.supportFragmentManager.popBackStackImmediate(id, flags)
    }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.popBackStackImmediate(
    name: String,
    flags: Int = 0
): Boolean {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var ret = false
    instrumentation.runOnMainSync {
        ret = activity.supportFragmentManager.popBackStackImmediate(name, flags)
    }
    return ret
}

fun ActivityTestRule<out FragmentActivity>.setContentView(@LayoutRes layoutId: Int) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.runOnMainSync { activity.setContentView(layoutId) }
}

fun assertChildren(container: ViewGroup, vararg fragments: Fragment) {
    val numFragments = fragments.size
    assertWithMessage("There aren't the correct number of fragment Views in its container")
        .that(container.childCount)
        .isEqualTo(numFragments)
    fragments.forEachIndexed { index, fragment ->
        assertWithMessage("Wrong Fragment View order for [$index]")
            .that(fragment.requireView())
            .isSameInstanceAs(container.getChildAt(index))
    }
}

// Transition test methods start
fun ActivityTestRule<out FragmentActivity>.findGreen(): View {
    return activity.findViewById(R.id.greenSquare)
}

fun ActivityTestRule<out FragmentActivity>.findBlue(): View {
    return activity.findViewById(R.id.blueSquare)
}

fun ActivityTestRule<out FragmentActivity>.findRed(): View? {
    return activity.findViewById(R.id.redSquare)
}

data class TransitionVerificationInfo(
    var epicenter: Rect? = null,
    val exitingViews: MutableList<View> = mutableListOf(),
    val enteringViews: MutableList<View> = mutableListOf()
)

fun TargetTracking.verifyAndClearTransition(block: TransitionVerificationInfo.() -> Unit) {
    val (epicenter, exitingViews, enteringViews) = TransitionVerificationInfo().apply { block() }

    if (epicenter == null) {
        assertThat(capturedEpicenter).isNull()
    } else {
        assertThat(capturedEpicenter).isEqualTo(epicenter)
    }
    assertThat(exitingTargets).containsExactlyElementsIn(exitingViews)
    assertThat(enteringTargets).containsExactlyElementsIn(enteringViews)
    clearTargets()
}

fun verifyNoOtherTransitions(fragment: TransitionFragment) {
    assertThat(fragment.enterTransition.enteringTargets.size).isEqualTo(0)
    assertThat(fragment.enterTransition.exitingTargets.size).isEqualTo(0)
    assertThat(fragment.exitTransition.enteringTargets.size).isEqualTo(0)
    assertThat(fragment.exitTransition.exitingTargets.size).isEqualTo(0)

    assertThat(fragment.reenterTransition.enteringTargets.size).isEqualTo(0)
    assertThat(fragment.reenterTransition.exitingTargets.size).isEqualTo(0)
    assertThat(fragment.returnTransition.enteringTargets.size).isEqualTo(0)
    assertThat(fragment.returnTransition.exitingTargets.size).isEqualTo(0)

    assertThat(fragment.sharedElementEnter.enteringTargets.size).isEqualTo(0)
    assertThat(fragment.sharedElementEnter.exitingTargets.size).isEqualTo(0)
    assertThat(fragment.sharedElementReturn.enteringTargets.size).isEqualTo(0)
    assertThat(fragment.sharedElementReturn.exitingTargets.size).isEqualTo(0)
}
// Transition test methods end

/**
 * Allocates until a garbage collection occurs.
 */
fun forceGC() {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
        // The following works on O+
        Runtime.getRuntime().gc()
        Runtime.getRuntime().gc()
        Runtime.getRuntime().runFinalization()
    } else {
        // The following works on older versions
        for (i in 0..1) {
            // Use a random index in the list to detect the garbage collection each time because
            // .get() may accidentally trigger a strong reference during collection.
            val leak = ArrayList<WeakReference<ByteArray>>()
            do {
                val arr = WeakReference(ByteArray(100))
                leak.add(arr)
            } while (leak[(Math.random() * leak.size).toInt()].get() != null)
        }
    }
}
