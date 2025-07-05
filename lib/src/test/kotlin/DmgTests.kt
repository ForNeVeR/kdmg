// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg.test

import kotlin.test.Test
import kotlin.test.assertTrue

class DmgTests {

    @Test
    fun `DMG file path should be passed`() {
        assertTrue(System.getProperty("kdmg.test.dmg").isNotBlank(), "kdmg.test.dmg should not be empty.")
    }
}
