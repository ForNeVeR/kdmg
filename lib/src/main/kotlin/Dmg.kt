package me.fornever.kdmg.util

import com.dd.plist.PropertyListParser
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Path

class Dmg {

    companion object {
        @JvmStatic
        fun read(path: Path): Dmg {
            FileInputStream(path.toFile()).use { stream ->
                stream.channel.use { channel ->
                    val trailer = tryReadTrailer(channel)
                    val xmlData = trailer?.let { readXmlData(channel, it) }
                }
            }

            return Dmg()
        }
    }
}

private data class XmlDataDescriptor(val offset: ULong, val length: ULong)

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
    return XmlDataDescriptor(xmlOffset, xmlLength)
}

private fun readXmlData(channel: FileChannel, xmlDataDescriptor: XmlDataDescriptor): Unit {
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
}
