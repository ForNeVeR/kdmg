// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg.app

import me.fornever.kdmg.util.Dmg
import org.radarbase.io.lzfse.LZFSEInputStream
import kotlin.io.path.Path

fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "dmg" -> {
            val path = Path(args[1])
            val dmg = Dmg.read(path)
            for (entry in dmg.descriptors) {
                println("name: ${entry.name}, table: ${entry.table}")
            }

            val hfsDescriptor = dmg.descriptors.single { it.name.contains("HFS") }
            val chunk = hfsDescriptor.table.chunks.first()
            val datum = dmg.getChunk(chunk)
            println("chunk data: ${datum.size} bytes")

            val stream = LZFSEInputStream(datum.inputStream())
            val newDatum = stream.readAllBytes()
            println("chunk data: ${newDatum.size} bytes")
        }
        else -> println("Usage:\n- dmg <path to dmg file> - print the DMG file diagnostics")
    }
}
