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

sealed interface CatalogFileDataRecord
class CatalogFileFolderRecord(
    val flags: UShort,
    val valence: UInt,
    val folderId: HFSCatalogNodeId,
    val createDate: UInt,
    val contentModDate: UInt,
    val attributeModDate: UInt,
    val accessDate: UInt,
    val backupDate: UInt,
    val permissions: HfsPlusBsdInfo,
    val userInfo: FolderInfo,
    val finderInfo: ExtendedFolderInfo,
    val textEncoding: UInt
): CatalogFileDataRecord

data class HfsPlusBsdInfo(
    val ownerId: UInt,
    val groupId: UInt,
    val adminFlags: UByte,
    val ownerFlags: UByte,
    val fileMode: UShort,
    val special: UInt
)

data class FolderInfo(
    val windowBounds: Rect,
    val finderFlags: UShort,
    val location: Point,
    val reservedField: UShort
)

data class Rect(
    val top: Short,
    val left: Short,
    val bottom: Short,
    val right: Short
)

data class Point(
    val v: Short,
    val h: Short
)

data class ExtendedFolderInfo(
    val reserved1: List<Short>,
    val extendedFinderFlags: UShort,
    val reserved2: Short,
    val putAwayFolderId: Int
)

internal fun MappedByteBuffer.readCatalogFileKey(): CatalogFileKey {
    val parentId = getUInt32()
    val name = getHfsUniStr255()
    return CatalogFileKey(parentId, name)
}

const val kHFSPlusFolderRecord: Short = 0x0001
const val kHFSPlusFileRecord: Short = 0x0002
const val kHFSPlusFolderThreadRecord: Short = 0x0003
const val kHFSPlusFileThreadRecord: Short = 0x0004

internal fun MappedByteBuffer.readCatalogFileDataRecord(): CatalogFileDataRecord {
    return when (val catalogFileRecordType = getShort()) {
        kHFSPlusFolderRecord -> readFolderRecord()
        kHFSPlusFileRecord -> readFileRecord()
        kHFSPlusFolderThreadRecord -> readFolderThreadRecord()
        kHFSPlusFileThreadRecord -> readFileThreadRecord()
        else -> error("Invalid catalog file record type: ${catalogFileRecordType.toHexString()}.")
    }
}

private fun MappedByteBuffer.readFolderRecord(): CatalogFileFolderRecord {
    return CatalogFileFolderRecord(
        getUInt16(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getHfsPlusBsdInfo(),
        getFolderInfo(),
        getExtendedFolderInfo(),
        getUInt32()
    ).also {
        getUInt32() // reserved
    }
}
