// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg.test

import me.fornever.kdmg.util.Dmg
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.test.*

class DmgTests {

    @Test
    fun `DMG file path should be passed`() {
        assertTrue(System.getProperty("kdmg.test.dmg").isNotBlank(), "kdmg.test.dmg should not be empty.")
    }

    private val dmgFile by lazy { Path(System.getProperty("kdmg.test.dmg")) }

    @Test
    fun `HFS descriptors should be read`() {
        val dmg = Dmg.read(dmgFile)
        val descriptorNames = dmg.descriptors.map { it.name }
        assertContentEquals(
            listOf(
                "Protective Master Boot Record (MBR : 0)",
                "GPT Header (Primary GPT Header : 1)",
                "GPT Partition Data (Primary GPT Table : 2)",
                " (Apple_Free : 3)",
                "EFI System Partition (C12A7328-F81F-11D2-BA4B-00A0C93EC93B : 4)",
                "disk image (Apple_HFS : 5)",
                " (Apple_Free : 6)",
                "GPT Partition Data (Backup GPT Table : 7)",
                "GPT Header (Backup GPT Header : 8)"
            ),
            descriptorNames,
            "All the descriptor names should be read correctly."
        )
    }

    @Test
    fun `HFS image should be extracted`() {
        val dmg = Dmg.read(dmgFile)
        val hfsDescriptor = assertNotNull(
            dmg.descriptors.singleOrNull { it.name.contains("Apple_HFS") },
            "There should be one and only one HFS descriptor."
        )
        val hfsFile = Files.createTempFile("kdmg", ".hfs")
        try {
            dmg.unpackBlkx(hfsDescriptor.table, hfsFile)
            assertHfsPlusSignature(hfsFile)
        } finally {
            Files.deleteIfExists(hfsFile)
        }
    }
}

private fun assertHfsPlusSignature(path: Path) {
    val file = path.inputStream()
    file.skip(1024)
    val data = String(file.readNBytes(2))
    assertEquals("H+", data)
}
