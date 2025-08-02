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

sealed interface CatalogDataRecord
class CatalogFolderRecord(
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
): CatalogDataRecord
class CatalogFileRecord(
    val flags: UShort,
    val fileId: HFSCatalogNodeId,
    val createDate: UInt,
    val contentModDate: UInt,
    val attributeModDate: UInt,
    val accessDate: UInt,
    val backupDate: UInt,
    val permissions: HfsPlusBsdInfo,
    val userInfo: FileInfo,
    val finderInfo: ExtendedFileInfo,
    val textEncoding: UInt,
    val dataFork: HfsPlusForkData,
    val resourceFork: HfsPlusForkData
) : CatalogDataRecord
class CatalogThreadRecord(
    parentId: HFSCatalogNodeId,
    nodeName: String
) : CatalogDataRecord

data class Point(
    val v: Short,
    val h: Short
)

data class Rect(
    val top: Short,
    val left: Short,
    val bottom: Short,
    val right: Short
)

typealias OSType = UInt

data class FileInfo(
    val fileType: OSType,
    val fileCreator: OSType,
    val finderFlags: UShort,
    val location: Point
)

data class ExtendedFileInfo(
    val extendedFinderFlags: UShort,
    val putAwayFolderId: Int
)

data class FolderInfo(
    val windowBounds: Rect,
    val finderFlags: UShort,
    val location: Point,
    val reservedField: UShort
)

data class ExtendedFolderInfo(
    val scrollPosition: Point,
    val extendedFinderFlags: UShort,
    val putAwayFolderId: Int
)

data class HfsPlusBsdInfo(
    val ownerId: UInt,
    val groupId: UInt,
    val adminFlags: UByte,
    val ownerFlags: UByte,
    val fileMode: UShort,
    val special: UInt
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

internal fun MappedByteBuffer.readCatalogFileDataRecord(): CatalogDataRecord {
    return when (val catalogFileRecordType = getShort()) {
        kHFSPlusFolderRecord -> readFolderRecord()
        kHFSPlusFileRecord -> readFileRecord()
        kHFSPlusFolderThreadRecord -> readThreadRecord()
        kHFSPlusFileThreadRecord -> readThreadRecord()
        else -> error("Invalid catalog file record type: ${catalogFileRecordType.toHexString()}.")
    }
}

private fun MappedByteBuffer.readFolderRecord(): CatalogFolderRecord {
    return CatalogFolderRecord(
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

private fun MappedByteBuffer.readFileRecord(): CatalogFileRecord {
    return CatalogFileRecord(
        getUInt16().also {
            getUInt32() // reserved1
        },
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getUInt32(),
        getHfsPlusBsdInfo(),
        getFileInfo(),
        getExtendedFileInfo(),
        getUInt32().also {
            getUInt32() // reserved2
        },
        readForkData(),
        readForkData()
    )
}

private fun MappedByteBuffer.readThreadRecord(): CatalogThreadRecord {
    getShort() // reserved
    return CatalogThreadRecord(
        getUInt32(),
        getHfsUniStr255()
    )
}

private fun MappedByteBuffer.getHfsPlusBsdInfo(): HfsPlusBsdInfo {
    return HfsPlusBsdInfo(
        getUInt32(),
        getUInt32(),
        getUInt8(),
        getUInt8(),
        getUInt16(),
        getUInt32()
    )
}

private fun MappedByteBuffer.getFolderInfo(): FolderInfo {
    return FolderInfo(
        getRect(),
        getUInt16(),
        getPoint(),
        getUInt16()
    )
}

private fun MappedByteBuffer.getExtendedFolderInfo(): ExtendedFolderInfo {
    return ExtendedFolderInfo(
        getPoint().also {
            getInt() // reserved1
        },
        getUInt16().also {
            getShort() // reserved2
        },
        getInt()
    )
}

private fun MappedByteBuffer.getFileInfo(): FileInfo {
    return FileInfo(
        getUInt32(),
        getUInt32(),
        getUInt16(),
        getPoint()
    ).also {
        getUInt16() // reservedField
    }
}

private fun MappedByteBuffer.getExtendedFileInfo(): ExtendedFileInfo {
    repeat(4) {
        getShort()
    } // reserved1
    return ExtendedFileInfo(
        getUInt16().also {
            getShort() // reserved2
        },
        getInt()
    )
}

private fun MappedByteBuffer.getRect(): Rect {
    return Rect(
        getShort(),
        getShort(),
        getShort(),
        getShort()
    )
}

private fun MappedByteBuffer.getPoint(): Point {
    return Point(
        getShort(),
        getShort()
    )
}
