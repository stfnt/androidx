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

package androidx.core.os;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Handler;
import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link HandlerExecutor}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class HandlerExecutorTest {

    private static final long TIMEOUT_MS = 5000;

    @Test
    public void testExecutor() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        HandlerExecutor executor = new HandlerExecutor(new Handler(Looper.getMainLooper()));
        executor.execute(new Runnable() {
            @Override
            public void run() {
                latch.countDown();
            }
        });
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testConstructor_Null() {
        try {
            new HandlerExecutor(null);
            fail();
        } catch (NullPointerException e) {
            // pass
        }
    }

    @Test
    public void testExecute_Null() {
        HandlerExecutor executor = new HandlerExecutor(new Handler(Looper.getMainLooper()));
        try {
            executor.execute(null);
            fail();
        } catch (NullPointerException e) {
            // pass
        }
    }
}

