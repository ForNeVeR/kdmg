package me.fornever.kdmg.util

import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import kotlin.io.path.writeBytes

// TODO: Check endianness
class HfsPlus(val path: Path) {

    fun readHeader(): VolumeHeader {
        return path.toFile().inputStream().use { stream ->
            stream.channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, 1024, 512).readHeader()
            }
        }
    }
    fun extractCatalogFile(header: VolumeHeader, destination: Path) {
        return path.toFile().inputStream().use { stream ->
            stream.channel.use { channel ->
                destination.writeBytes(channel.readFile(header, header.catalogFile))
            }
        }
    }

    companion object {
        @JvmStatic
        fun parseCatalogFile(catalog: Path): BTreeHeader {
            return catalog.toFile().inputStream().use { stream ->
                stream.channel.use { channel ->
                    channel.readBTreeHeader()
                }
            }
        }
    }
}

private val kHFSPlusSigWord = 0x482bu.toUShort() // 'H+'

private fun MappedByteBuffer.readHeader(): VolumeHeader {
    val signature = getShort().toUShort()
    if (signature != kHFSPlusSigWord) {
        throw IllegalArgumentException("Invalid signature: " + signature.toString(16))
    }

    val version = getShort().toUShort()
    val attributes = getInt().toUInt()
    val lastMountedVersion = getInt().toUInt()
    val journalInfoBlock = getInt().toUInt()
    val createDate = getInt().toUInt()
    val modifyDate = getInt().toUInt()
    val backupDate = getInt().toUInt()
    val checkedDate = getInt().toUInt()
    val fileCount = getInt().toUInt()
    val folderCount = getInt().toUInt()
    val blockSize = getInt().toUInt()
    val totalBlocks = getInt().toUInt()
    val freeBlocks = getInt().toUInt()
    val nextAllocation = getInt().toUInt()
    val rsrcClumpSize = getInt().toUInt()
    val dataClumpSize = getInt().toUInt()
    val nextCatalogId = getInt().toUInt()
    val writeCount = getInt().toUInt()
    val encodingsBitmap = getLong().toULong()
    val finderInfo = Array(8) { getInt().toUInt() }

    val allocationFile = readForkData()
    val extentsFile = readForkData()
    val catalogFile = readForkData()
    val attributesFile = readForkData()
    val startupFile = readForkData()

    return VolumeHeader(
        signature,
        version,
        attributes,
        lastMountedVersion,
        journalInfoBlock,
        createDate,
        modifyDate,
        backupDate,
        checkedDate,
        fileCount,
        folderCount,
        blockSize,
        totalBlocks,
        freeBlocks,
        nextAllocation,
        rsrcClumpSize,
        dataClumpSize,
        nextCatalogId,
        writeCount,
        encodingsBitmap,
        finderInfo,
        allocationFile,
        extentsFile,
        catalogFile,
        attributesFile,
        startupFile
    )
}

private fun MappedByteBuffer.readForkData(): HfsPlusForkData {
    val logicalSize = getLong().toULong()
    val clumpSize = getInt().toUInt()
    val totalBlocks = getInt().toUInt()
    val extents = mutableListOf<HfsPlusExtentDescriptor>()
    repeat(8) {
        val startBlock = getInt().toUInt()
        val blockCount = getInt().toUInt()
        if (startBlock != 0u || blockCount != 0u)
            extents.add(HfsPlusExtentDescriptor(startBlock, blockCount))
    }

    return HfsPlusForkData(logicalSize, clumpSize, totalBlocks, extents.toTypedArray())
}

private fun FileChannel.readFile(header: VolumeHeader, fork: HfsPlusForkData): ByteArray {
    if (fork.extents.size >= 8) TODO("Handle extents in the extent overflow file.")
    val result = ByteArray(fork.logicalSize.toInt())
    var position = 0
    for (extent in fork.extents) {
        val readFrom = extent.startBlock * header.blockSize
        val size = extent.blockCount * header.blockSize
        val mapping = map(FileChannel.MapMode.READ_ONLY, readFrom.toLong(), size.toLong())
        val data = ByteArray(size.toInt()).apply(mapping::get)
        data.copyInto(result, position)
        position += data.size
    }

    if (position != result.size) error("Read $position bytes, expected ${fork.logicalSize}.")
    return result
}

typealias HfsCatalogNodeId = UInt

data class VolumeHeader(
    val signature: UShort,
    val version: UShort,
    val attributes: UInt,
    val lastMountedVersion: UInt,
    val journalInfoBlock: UInt,

    val createDate: UInt,
    val modifyDate: UInt,
    val backupDate: UInt,
    val checkedDate: UInt,

    val fileCount: UInt,
    val folderCount: UInt,

    val blockSize: UInt,
    val totalBlocks: UInt,
    val freeBlocks: UInt,

    val nextAllocation: UInt,
    val rsrcClumpSize: UInt,
    val dataClumpSize: UInt,
    val nextCatalogId: HfsCatalogNodeId,

    val writeCount: UInt,
    val encodingsBitmap: ULong,

    val finderInfo: Array<UInt>,

    val allocationFile: HfsPlusForkData,
    val extentsFile: HfsPlusForkData,
    val catalogFile: HfsPlusForkData,
    val attributesFile: HfsPlusForkData,
    val startupFile: HfsPlusForkData
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VolumeHeader

        if (signature != other.signature) return false
        if (version != other.version) return false
        if (attributes != other.attributes) return false
        if (lastMountedVersion != other.lastMountedVersion) return false
        if (journalInfoBlock != other.journalInfoBlock) return false
        if (createDate != other.createDate) return false
        if (modifyDate != other.modifyDate) return false
        if (backupDate != other.backupDate) return false
        if (checkedDate != other.checkedDate) return false
        if (fileCount != other.fileCount) return false
        if (folderCount != other.folderCount) return false
        if (blockSize != other.blockSize) return false
        if (totalBlocks != other.totalBlocks) return false
        if (freeBlocks != other.freeBlocks) return false
        if (nextAllocation != other.nextAllocation) return false
        if (rsrcClumpSize != other.rsrcClumpSize) return false
        if (dataClumpSize != other.dataClumpSize) return false
        if (nextCatalogId != other.nextCatalogId) return false
        if (writeCount != other.writeCount) return false
        if (encodingsBitmap != other.encodingsBitmap) return false
        if (!finderInfo.contentEquals(other.finderInfo)) return false
        if (allocationFile != other.allocationFile) return false
        if (extentsFile != other.extentsFile) return false
        if (catalogFile != other.catalogFile) return false
        if (attributesFile != other.attributesFile) return false
        if (startupFile != other.startupFile) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signature.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + lastMountedVersion.hashCode()
        result = 31 * result + journalInfoBlock.hashCode()
        result = 31 * result + createDate.hashCode()
        result = 31 * result + modifyDate.hashCode()
        result = 31 * result + backupDate.hashCode()
        result = 31 * result + checkedDate.hashCode()
        result = 31 * result + fileCount.hashCode()
        result = 31 * result + folderCount.hashCode()
        result = 31 * result + blockSize.hashCode()
        result = 31 * result + totalBlocks.hashCode()
        result = 31 * result + freeBlocks.hashCode()
        result = 31 * result + nextAllocation.hashCode()
        result = 31 * result + rsrcClumpSize.hashCode()
        result = 31 * result + dataClumpSize.hashCode()
        result = 31 * result + nextCatalogId.hashCode()
        result = 31 * result + writeCount.hashCode()
        result = 31 * result + encodingsBitmap.hashCode()
        result = 31 * result + finderInfo.contentHashCode()
        result = 31 * result + allocationFile.hashCode()
        result = 31 * result + extentsFile.hashCode()
        result = 31 * result + catalogFile.hashCode()
        result = 31 * result + attributesFile.hashCode()
        result = 31 * result + startupFile.hashCode()
        return result
    }
}

typealias HfsPlusExtentRecord = Array<HfsPlusExtentDescriptor>

data class HfsPlusForkData(
    val logicalSize: ULong,
    val clumpSize: UInt,
    val totalBlocks: UInt,
    val extents: HfsPlusExtentRecord
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HfsPlusForkData

        if (logicalSize != other.logicalSize) return false
        if (clumpSize != other.clumpSize) return false
        if (totalBlocks != other.totalBlocks) return false
        if (!extents.contentEquals(other.extents)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = logicalSize.hashCode()
        result = 31 * result + clumpSize.hashCode()
        result = 31 * result + totalBlocks.hashCode()
        result = 31 * result + extents.contentHashCode()
        return result
    }
}

data class HfsPlusExtentDescriptor(
    val startBlock: UInt,
    val blockCount: UInt
)

fun FileChannel.readBTreeHeader(): BTreeHeader {
    val buffer = map(FileChannel.MapMode.READ_ONLY, 0, size())
    return BTreeHeader(
        buffer.readBTreeNodeDescriptor(), // TODO: Check whether it is a leaf or an index node
        buffer.readBTreeHeaderRecord()
    )
}

private fun MappedByteBuffer.readBTreeNodeDescriptor(): BTreeNodeDescriptor {
    val header = BTreeNodeDescriptor(
        fLink = getInt().toUInt(),
        bLink = getInt().toUInt(),
        kind = get(),
        height = get().toUByte(),
        numRecords = getShort().toUShort()
    )
    getShort() // reserved field
    return header
}

data class BTreeNodeDescriptor(
    val fLink: UInt,
    val bLink: UInt,
    val kind: Byte,
    val height: UByte,
    val numRecords: UShort
)

data class BTreeHeader(
    val descriptor: BTreeNodeDescriptor,
    val headerRecord: BTreeHeaderRecord
)

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
)

fun MappedByteBuffer.readBTreeHeaderRecord(): BTreeHeaderRecord {
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
