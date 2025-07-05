// SPDX-FileCopyrightText: 2025 kdmg contributors <https://github.com/ForNeVeR/kdmg>
//
// SPDX-License-Identifier: Apache-2.0

package me.fornever.kdmg.util

import com.dd.plist.*
import org.radarbase.io.lzfse.LZFSEInputStream
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.appendBytes
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists

// TODO: Investigate if we read numbers properly, in Big Endian
class Dmg(val path: Path, val header: XmlDataDescriptor, val descriptors: List<BlkxDescriptor>) {

    companion object {
        @JvmStatic
        fun read(path: Path): Dmg {
            FileInputStream(path.toFile()).use { stream ->
                stream.channel.use { channel ->
                    val trailer = tryReadTrailer(channel)
                    val xmlData = trailer?.let { readXmlData(channel, it) } ?: error("Read error")
                    return Dmg(path, trailer, xmlData)
                }
            }
        }
    }

    fun getChunk(chunk: BlkxChunkEntry): ByteArray {
        FileInputStream(path.toFile()).use { stream ->
            stream.channel.use { channel ->
                val dataFork = channel.map(FileChannel.MapMode.READ_ONLY, header.dataForkOffset.toLong(), header.dataForkLength.toLong()) // TODO: Checked cast

                val buffer = ByteArray(chunk.compressedLength.toInt())
                dataFork.get(chunk.compressedOffset.toInt(), buffer)
                return buffer
            }
        }
    }

    private fun uncompressChunk(chunk: BlkxChunkEntry, data: ByteArray): ByteArray {
        when (chunk.entryType) {
            2U -> if (data.isEmpty()) return ByteArray((chunk.sectorCount * SECTOR_SIZE_BYTES).toInt())
            0x80000007U -> {
                val stream = LZFSEInputStream(data.inputStream())
                val newDatum = stream.readAllBytes()
                println("chunk data: ${newDatum.size} bytes")
                return newDatum
            }
            else -> {}
        }

        error("Unsupported DMG chunk entry type: ${chunk.entryType} (size: ${data.size}).")
    }

    fun unpackBlkx(table: BlkxTable, destination: Path) {
        var pos = 0UL
        destination.deleteIfExists()
        destination.createFile()

        for (chunk in table.chunks) {
            if (chunk.entryType == 0xFFFFFFFFU) break // last entry // TODO: Check that it is indeed last

            val newPos = chunk.sectorNumber * SECTOR_SIZE_BYTES
            if (newPos != pos) error("newPos = $newPos, pos = $pos, expected equal numbers")

            pos = newPos
            val compressedData = getChunk(chunk)
            val dataToWrite = uncompressChunk(chunk, compressedData)

            val expectedBytes = chunk.sectorCount * SECTOR_SIZE_BYTES
            if (expectedBytes != dataToWrite.size.toULong()) {
                error("expectedBytes = $expectedBytes, actual = ${dataToWrite.size}.")
            }
            destination.appendBytes(dataToWrite)

            println("$destination: ${dataToWrite.size} bytes written.")
            pos += dataToWrite.size.toUInt()
        }

        val expectedSize = table.sectorCount * SECTOR_SIZE_BYTES
        if (expectedSize != pos) error("File \"$destination\" cannot be unpacked: expected size ${expectedSize}, actual $pos.")
    }
}

private const val SECTOR_SIZE_BYTES = 512U

data class XmlDataDescriptor(val offset: ULong, val length: ULong, val dataForkOffset: ULong, val dataForkLength: ULong)

/**
 * https://newosxbook.com/DMG.html
 */
private fun tryReadTrailer(channel: FileChannel): XmlDataDescriptor? {
    if (channel.size() < 512) {
        return null
    }

    val buffer = channel.map(FileChannel.MapMode.READ_ONLY, channel.size() - 512, 512)
    val signature = ByteArray(4).apply(buffer::get)
    if (!signature.contentEquals("koly".toByteArray())) {
        return null
    }

    val version = buffer.getInt().toUInt()
    val headerSize = buffer.getInt().toUInt()
    val flags = buffer.getInt().toUInt()
    val runningForkDataOffset = buffer.getLong().toULong()
    val dataForkOffset = buffer.getLong().toULong()
    val dataForkLength = buffer.getLong().toULong()
    val resourceForkOffset = buffer.getLong().toULong()
    val resourceForkLength = buffer.getLong().toULong()
    val segmentNumber = buffer.getInt().toUInt()
    val segmentCount = buffer.getInt().toUInt()
    val segmentId = ByteArray(16).apply(buffer::get)

    val dataChecksumType = buffer.getInt().toUInt()
    val dataChecksumSize = buffer.getInt().toUInt()
    val dataChecksum = ByteArray(32 * 4).apply(buffer::get)

    val xmlOffset = buffer.getLong().toULong()
    val xmlLength = buffer.getLong().toULong()
    @Suppress("UnusedVariable") val reserved = ByteArray(120).apply(buffer::get)

    val checksumType = buffer.getInt().toUInt()
    val checksumSize = buffer.getInt().toUInt()
    val checksum = ByteArray(32 * 4).apply(buffer::get)

    val imageVariant = buffer.getInt().toUInt()
    val sectorCount = buffer.getLong().toUInt()

    println("Version: $version")
    println("Header size: $headerSize")
    println("Flags: $flags")
    println("Running fork data offset: $runningForkDataOffset")
    println("Data fork offset: $dataForkOffset")
    println("Data fork length: $dataForkLength")
    println("Resource fork offset: $resourceForkOffset")
    println("Resource fork length: $resourceForkLength")
    println("Segment number: $segmentNumber")
    println("Segment count: $segmentCount")
    println("Segment id: $segmentId")
    println("Data checksum type: $dataChecksumType")
    println("Data checksum size: $dataChecksumSize")
    println("Data checksum: $dataChecksum")
    println("XML offset: $xmlOffset")
    println("XML length: $xmlLength")
    println("Checksum type: $checksumType")
    println("Checksum size: $checksumSize")
    println("Checksum: $checksum")
    println("Image variant: $imageVariant")
    println("Sector count: $sectorCount")

    // Also, 3 reserved UInt32 items here.
    return XmlDataDescriptor(xmlOffset, xmlLength, dataForkOffset, dataForkLength)
}

/*
typedef struct {
        uint32_t Signature;          // Magic ('mish')
        uint32_t Version;            // Current version is 1
        uint64_t SectorNumber;       // Starting disk sector in this blkx descriptor
        uint64_t SectorCount;        // Number of disk sectors in this blkx descriptor

        uint64_t DataOffset;     
        uint32_t BuffersNeeded;
        uint32_t BlockDescriptors;   // Number of descriptors

        uint32_t reserved1;
        uint32_t reserved2;
        uint32_t reserved3;
        uint32_t reserved4;
        uint32_t reserved5;
        uint32_t reserved6;

        UDIFChecksum checksum;

        uint32_t NumberOfBlockChunks; 
        BLKXChunkEntry [0];
} __attribute__((__packed__)) BLKXTable;

// Where each  BLXKRunEntry is defined as follows:

typedef struct {
        uint32_t EntryType;         // Compression type used or entry type (see next table)
        uint32_t Comment;           // "+beg" or "+end", if EntryType is comment (0x7FFFFFFE). Else reserved.
        uint64_t SectorNumber;      // Start sector of this chunk
        uint64_t SectorCount;       // Number of sectors in this chunk
        uint64_t CompressedOffset;  // Start of chunk in data fork
        uint64_t CompressedLength;  // Count of bytes of chunk, in data fork
} __attribute__((__packed__)) BLKXChunkEntry;
 */

data class BlkxTable(
    val signature: UInt,      // 'mish'
    val version: UInt,        // Current version is 1
    val sectorNumber: ULong,  // Starting disk sector
    val sectorCount: ULong,   // Number of disk sectors
    val dataOffset: ULong,
    val buffersNeeded: UInt,
    val blockDescriptors: UInt,
    val checksum: ByteArray,  // UDIFChecksum
    val numberOfBlockChunks: UInt,
    val chunks: List<BlkxChunkEntry>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlkxTable

        if (signature != other.signature) return false
        if (version != other.version) return false
        if (sectorNumber != other.sectorNumber) return false
        if (sectorCount != other.sectorCount) return false
        if (dataOffset != other.dataOffset) return false
        if (buffersNeeded != other.buffersNeeded) return false
        if (blockDescriptors != other.blockDescriptors) return false
        if (!checksum.contentEquals(other.checksum)) return false
        if (numberOfBlockChunks != other.numberOfBlockChunks) return false
        return chunks == other.chunks
    }

    override fun hashCode(): Int {
        var result = signature.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + sectorNumber.hashCode()
        result = 31 * result + sectorCount.hashCode()
        result = 31 * result + dataOffset.hashCode()
        result = 31 * result + buffersNeeded.hashCode()
        result = 31 * result + blockDescriptors.hashCode()
        result = 31 * result + checksum.contentHashCode()
        result = 31 * result + numberOfBlockChunks.hashCode()
        result = 31 * result + chunks.hashCode()
        return result
    }
}

data class BlkxChunkEntry(
    val entryType: UInt,           // Compression type or entry type
    val comment: UInt,             // "+beg" or "+end" for comments, else reserved
    val sectorNumber: ULong,       // Start sector of chunk
    val sectorCount: ULong,        // Number of sectors in chunk
    val compressedOffset: ULong,   // Start of chunk in data fork
    val compressedLength: ULong    // Count of bytes of chunk in data fork
)

private fun parseBlkxDescriptor(data: ByteArray): BlkxTable {
    val buffer = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.BIG_ENDIAN)

    val signature = buffer.getInt().toUInt()
    require(signature == 0x6D697368u) { "Invalid signature: expected 'mish'" } // 'mish' in hex

    val version = buffer.getInt().toUInt()
    val sectorNumber = buffer.getLong().toULong()
    val sectorCount = buffer.getLong().toULong()
    val dataOffset = buffer.getLong().toULong()
    val buffersNeeded = buffer.getInt().toUInt()
    val blockDescriptors = buffer.getInt().toUInt()

    // Skip reserved fields
    repeat(6) { buffer.getInt() }

    // Read checksum (assuming it's a fixed size array, adjust size if needed)
    val checksum = ByteArray(136)
    buffer.get(checksum)

    val numberOfBlockChunks = buffer.getInt().toUInt()

    // Read chunk entries
    val chunks = mutableListOf<BlkxChunkEntry>()
    repeat(numberOfBlockChunks.toInt()) {
        val chunk = BlkxChunkEntry(
            entryType = buffer.getInt().toUInt(),
            comment = buffer.getInt().toUInt(),
            sectorNumber = buffer.getLong().toULong(),
            sectorCount = buffer.getLong().toULong(),
            compressedOffset = buffer.getLong().toULong(),
            compressedLength = buffer.getLong().toULong()
        )
        chunks.add(chunk)
    }

    return BlkxTable(
        signature = signature,
        version = version,
        sectorNumber = sectorNumber,
        sectorCount = sectorCount,
        dataOffset = dataOffset,
        buffersNeeded = buffersNeeded,
        blockDescriptors = blockDescriptors,
        checksum = checksum,
        numberOfBlockChunks = numberOfBlockChunks,
        chunks = chunks
    )
}

data class BlkxDescriptor(val name: String, val table: BlkxTable)

private fun readXmlData(channel: FileChannel, xmlDataDescriptor: XmlDataDescriptor): List<BlkxDescriptor> {
    val data = channel.map(
        FileChannel.MapMode.READ_ONLY,
        xmlDataDescriptor.offset.toLong(), // TODO: Checked cast?
        xmlDataDescriptor.length.toLong()
    )
    val bytes = ByteArray(data.capacity()).apply(data::get)
    val xmlString = String(
        bytes,
        StandardCharsets.US_ASCII
    )
    println("XML received: $xmlString")

    val parsedData = PropertyListParser.parse(bytes)
    println("XML parsed: $parsedData")

    val resourceFork = (parsedData as NSDictionary)["resource-fork"] as NSDictionary
    val blkx = resourceFork["blkx"] as NSArray
    return blkx.array.map { entry ->
        entry as NSDictionary
        BlkxDescriptor(
            (entry["CFName"] as NSString).content,
            parseBlkxDescriptor((entry["Data"] as NSData).bytes())
        )
    }
}
