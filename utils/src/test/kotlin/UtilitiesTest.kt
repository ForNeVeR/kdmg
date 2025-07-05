// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package org.example.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.example.utils.*

internal class PrinterTest {

    @Test
    fun testMessage() {
        val message = "message"
        val testPrinter = Printer(message)
        assertEquals(testPrinter.message, message)
    }

    @Test
    fun testSerialization() {
        val message = "message"
        val json1 = Json.encodeToString(Printer(message))
        val json2 = Json.encodeToString(Printer(message))
        assertEquals(json1, json2)
    }
}