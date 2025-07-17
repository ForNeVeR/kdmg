// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg

import java.nio.MappedByteBuffer

typealias HFSCatalogNodeId = UInt

data class CatalogFileKey(
    val parentId: HFSCatalogNodeId,
    val nodeName: String
)

internal fun MappedByteBuffer.readCatalogFileKey(keyLengthInBytes: UShort): CatalogFileKey {
    val parentId = getUInt32()
    val name = getHfsUniStr255()
    return CatalogFileKey(parentId, name)
}

