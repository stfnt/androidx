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

package androidx.datastore.preferences

import androidx.datastore.core.CorruptionException
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
class PreferencesSerializerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var testFile: File
    private val preferencesSerializer = PreferencesSerializer

    @Before
    fun setUp() {
        testFile = tmp.newFile()
    }

    @Test
    fun testWriteAndReadString() {
        val stringKey = preferencesKey<String>("string_key")

        val prefs = preferencesOf(
            stringKey to "string1"
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadStringSet() {
        val stringSetKey = preferencesSetKey<String>("string_set_key")

        val prefs = preferencesOf(
            stringSetKey to setOf("string1", "string2", "string3")
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadLong() {
        val longKey = preferencesKey<Long>("long_key")

        val prefs = preferencesOf(
            longKey to (1 shr 50)
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadInt() {
        val intKey = preferencesKey<Int>("int_key")

        val prefs = preferencesOf(
            intKey to 3
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testWriteAndReadBoolean() {
        val booleanKey = preferencesKey<Boolean>("boolean_key")

        val prefs = preferencesOf(
            booleanKey to true
        )

        testFile.outputStream().use {
            preferencesSerializer.writeTo(prefs, it)
        }

        val readPrefs = testFile.inputStream().use {
            preferencesSerializer.readFrom(it)
        }

        assertEquals(prefs, readPrefs)
    }

    @Test
    fun testThrowsCorruptionException() {
        // Not a valid proto - protos cannot start with a 0 byte.
        testFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4))

        assertFailsWith<CorruptionException> {
            testFile.inputStream().use {
                preferencesSerializer.readFrom(it)
            }
        }
    }
}