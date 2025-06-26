// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg.app

import me.fornever.kdmg.Dmg
import me.fornever.kdmg.util.HfsPlus
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

fun main(args: Array<String>) {
    when (args.getOrNull(0)) {
        "dmg" -> {
            val path = Path(args[1])
            val dmg = Dmg.read(path)
            for (entry in dmg.descriptors) {
                println("name: ${entry.name}, table: ${entry.table}")
            }

            val hfsDescriptor = dmg.descriptors.single { it.name.contains("Apple_HFS") }
            val hfsPath = Path("build/image.hfs").also {
                println("Saving file as \"${it.absolutePathString()}\".")
            }
            dmg.unpackBlkx(hfsDescriptor.table, hfsPath)
            println("File \"$hfsPath\" written.")
        }
        "hfs+" -> {
            val path = Path(args[1])
            val hfsPlus = HfsPlus(path)
            val header = hfsPlus.readHeader()
            println(header)

            val catalogPath = Path("build/catalog.cat")
            println("Saving catalog as \"${catalogPath.absolutePathString()}\".")
            hfsPlus.extractCatalogFile(header, catalogPath)
            println("File \"$catalogPath\" written successfully.")
        }
        "catalog" -> {
            val path = Path(args[1])
            val result = HfsPlus.parseCatalogFile(path)
            println("Catalog parse results: $result")
        }
        else -> println("Usage:\n- dmg <path to dmg file> - print the DMG file diagnostics and unpack it\n- hfs+ <path to hfs+ file> - unpack a HFS+ file")
    }
}
