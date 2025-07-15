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

// TODO: Catalog file data types: key and data record, are kept in BTreePointerRecord and BTreeDataRecord
data class BTreePointerRecord<TKey>(
    val key: TKey,
    val nodeNumber: UInt
) : BTreeRecord

data class BTreeDataRecord<TKey, TData>(
    val key: TKey,
    val data: TData
) : BTreeRecord

internal fun FileChannel.readHeaderNode(): BTreeNode {
    val buffer = map(FileChannel.MapMode.READ_ONLY, 0, size())
    val descriptor = buffer.readNodeDescriptor()
    if (descriptor.kind != BTreeNodeKind.Header) error("Incorrect node kind for the header node: ${descriptor.kind}.")
    if (descriptor.numRecords != 3.toUShort())
        error("Incorrect number of records in the header node: ${descriptor.numRecords}.")

    val headerRecordOffset = buffer.position().toUShort()
    val headerRecord = buffer.readHeaderRecord()

    val userDataRecordOffset = buffer.position().toUShort()
    val userDataRecord = buffer.readUserDataRecord()

    // TODO: Assert current position = 256 (minus record offsets in the end?)
    val mapSize = (headerRecord.nodeSize - 256.toUShort()).toUShort()
    val mapRecordOffset = buffer.position().toUShort()
    val mapRecord = buffer.readMapRecord(mapSize)

    val freeSpaceOffset = buffer.position().toUShort()

    fun readAndAssertOffset(offset: UShort, name: String) {
        val readOffset = buffer.getShort().toUShort()
        if (readOffset != offset) error("Offset of $name is incorrect: expected $offset, got $readOffset.")
    }
    readAndAssertOffset(freeSpaceOffset, "free space")
    readAndAssertOffset(mapRecordOffset, "map record")
    readAndAssertOffset(userDataRecordOffset, "user data record")
    readAndAssertOffset(headerRecordOffset, "header record")

    return BTreeNode(
        descriptor,
        listOf(headerRecord, userDataRecord, mapRecord)
    )
}

private fun <TKey, TData> FileChannel.readNode(
    header: BTreeHeaderRecord,
    number: UInt,
    readKey: MappedByteBuffer.() -> TKey,
    readData: MappedByteBuffer.() -> TData
): BTreeNode {
    val nodeOffset = header.nodeSize * number
    val buffer = map(FileChannel.MapMode.READ_ONLY, nodeOffset.toLong(), header.nodeSize.toLong())
    val descriptor = buffer.readNodeDescriptor()
    val node = when (descriptor.kind) {
        BTreeNodeKind.Header -> error("Unexpected node kind: ${descriptor.kind}. Should only be read at number 0, not ${number}.")
        BTreeNodeKind.Leaf, BTreeNodeKind.Index -> BTreeNode(
            descriptor,
            buffer.readKeyedRecords(header, descriptor, readKey, readData)
        )
        else -> error("Unexpected node kind: ${descriptor.kind}.")
    }

    // TODO: Verify the free space offset
    // TODO: Verify the node offsets

    return node
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

private val kBTBigKeysMask: UInt = 0x00000002.toUInt()
private val kBTVariableIndexKeysMask: UInt = 0x00000004.toUInt()

private fun <TKey, TData> MappedByteBuffer.readKeyedRecords(
    header: BTreeHeaderRecord,
    descriptor: BTreeNodeDescriptor,
    readKey: MappedByteBuffer.() -> TKey,
    readData: MappedByteBuffer.() -> TData
): List<BTreeRecord> {
    val usesBigKey = header.attributes and kBTBigKeysMask != 0u
    val nodeKind = descriptor.kind
    return buildList(descriptor.numRecords.toInt()) {
        val statedKeyLength = if (usesBigKey) getShort().toUShort() else get().toUShort()
        val actualKeyLength = when (nodeKind) {
            BTreeNodeKind.Leaf -> statedKeyLength
            BTreeNodeKind.Index -> {
                val variableIndex = header.attributes and kBTVariableIndexKeysMask != 0u
                if (variableIndex) statedKeyLength else header.maxKeyLength
            }
            else -> error("Unexpected node kind: $nodeKind.")
        }

        if (position() % 2 != 0) {
            // read pad byte
            @Suppress("UnusedVariable", "unused") val padByte: Byte = get()
        }

        val key = ByteArray(actualKeyLength.toInt()).apply(::get)
        when (nodeKind) {
            BTreeNodeKind.Index -> readIndexNodeRecord(readKey)
            BTreeNodeKind.Leaf -> readDataRecord(readKey, readData)
            else -> error("Unexpected node kind: $nodeKind.")
        }
    }
}

private fun <TKey> MappedByteBuffer.readIndexNodeRecord(
    readKey: MappedByteBuffer.() -> TKey
): BTreePointerRecord<TKey> {
    return BTreePointerRecord(
        readKey(),
        getUInt32()
    )
}

private fun <TKey, TData> MappedByteBuffer.readDataRecord(
    readKey: MappedByteBuffer.() -> TKey,
    readData: MappedByteBuffer.() -> TData
): BTreeDataRecord<TKey, TData> {
    return BTreeDataRecord(
        readKey(),
        readData()
    )
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
