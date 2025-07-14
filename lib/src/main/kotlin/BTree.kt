// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg

import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class BTreeNode(
    val descriptor: BTreeNodeDescriptor,
    val records: List<BTreeRecord>
)

enum class BTreeNodeKind {
    Leaf,
    Index,
    Header,
    Map;

    companion object {
        @JvmStatic
        fun of(byte: Byte): BTreeNodeKind = when (byte) {
            0xFF.toByte() -> Leaf
            0.toByte() -> Index
            1.toByte() -> Header
            2.toByte() -> Map
            else -> error("Unknown BTree node kind: $byte")
        }
    }
}

data class BTreeNodeDescriptor(
    val fLink: UInt,
    val bLink: UInt,
    val kind: BTreeNodeKind,
    val height: UByte,
    val numRecords: UShort
)

sealed interface BTreeRecord

data class BTreeHeaderRecord(
    val treeDepth: UShort,
    val rootNode: UInt,
    val leafRecords: UInt,
    val firstLeafNode: UInt,
    val lastLeafNode: UInt,
    val nodeSize: UShort,
    val maxKeyLength: UShort,
    val totalNodes: UInt,
    val freeNodes: UInt,
    val clumpSize: UInt,
    val btreeType: UByte,
    val keyCompareType: UByte,
    val attributes: UInt
) : BTreeRecord

data class BTreeUserDataRecord(
    val data: ByteArray
) : BTreeRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BTreeUserDataRecord

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

data class BTreeMapRecord(
    val data: ByteArray
) : BTreeRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BTreeUserDataRecord

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

internal fun FileChannel.readHeaderNode(): BTreeNode {
    val buffer = map(FileChannel.MapMode.READ_ONLY, 0, size())
    val descriptor = buffer.readNodeDescriptor()
    if (descriptor.kind != BTreeNodeKind.Header) error("Incorrect node kind for the header node: ${descriptor.kind}.")

    val headerRecord = buffer.readHeaderRecord()
    val userDataRecord = buffer.readUserDataRecord()
    // TODO: Assert current position = 256 (minus record offsets in the end?)
    val mapSize = (headerRecord.nodeSize - 256.toUShort()).toUShort()
    val mapRecord = buffer.readMapRecord(mapSize)
    // TODO: Assert offsets at the end of the node — should point to the records.
    return BTreeNode(
        descriptor,
        listOf(headerRecord, userDataRecord, mapRecord)
    )
}

private fun MappedByteBuffer.readNodeDescriptor(): BTreeNodeDescriptor {
    val header = BTreeNodeDescriptor(
        fLink = getInt().toUInt(),
        bLink = getInt().toUInt(),
        kind = BTreeNodeKind.of(get()),
        height = get().toUByte(),
        numRecords = getShort().toUShort()
    )
    getShort() // reserved field
    return header
}

private fun MappedByteBuffer.readHeaderRecord(): BTreeHeaderRecord {
    val treeDepth = getShort().toUShort()
    val rootNode = getInt().toUInt()
    val leafRecords = getInt().toUInt()
    val firstLeafNode = getInt().toUInt()
    val lastLeafNode = getInt().toUInt()
    val nodeSize = getShort().toUShort()
    val maxKeyLength = getShort().toUShort()
    val totalNodes = getInt().toUInt()
    val freeNodes = getInt().toUInt()
    getShort() // reserved1
    val clumpSize = getInt().toUInt()
    val btreeType = get().toUByte()
    val keyCompareType = get().toUByte()
    val attributes = getInt().toUInt()
    Array(16) { getInt().toUInt() } // reserved3

    return BTreeHeaderRecord(
        treeDepth,
        rootNode,
        leafRecords,
        firstLeafNode,
        lastLeafNode,
        nodeSize,
        maxKeyLength,
        totalNodes,
        freeNodes,
        clumpSize,
        btreeType,
        keyCompareType,
        attributes
    )
}

internal fun MappedByteBuffer.readUserDataRecord(): BTreeUserDataRecord {
    val result = ByteArray(128).apply { get(this) }
    return BTreeUserDataRecord(result)
}

internal fun MappedByteBuffer.readMapRecord(size: UShort): BTreeMapRecord {
    val result = ByteArray(size.toInt()).apply { get(this) }
    return BTreeMapRecord(result)
}
