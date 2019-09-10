package xyz.chunkstories.world.region.format

import org.slf4j.LoggerFactory
import xyz.chunkstories.world.chunk.ChunkCompressedData
import xyz.chunkstories.world.region.RegionImplementation
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

object RegionFileSerialization {
    fun loadRegion(file: File, region: RegionImplementation) {
        val dis = DataInputStream(file.inputStream())
        readHeaderAndDispatch(dis, region)
        dis.close()
    }

    fun saveRegion(file: File, region: RegionImplementation) {
        val dos = DataOutputStream(file.outputStream())
        writeHeader(dos)
        writeContents(dos, region)
        dos.close()
    }

    fun readHeaderAndDispatch(dis: DataInputStream, region: RegionImplementation) {
        val magicNumber = dis.readLong()

        if(magicNumber != 6003953969960732739L)
            throw Exception("Whatever this is, it's not a chunk stories region file.")

        val versionNumber = dis.readInt()
        val writeTimestamp = dis.readLong()

        if(versionNumber == 0x2E)
            readContents(dis, region)
        else
            throw Exception("Unsupported revision: $versionNumber")
    }

    fun readContents(dis: DataInputStream, region: RegionImplementation) {
        // First load the compressed chunk data sizes
        val chunksSizes = IntArray(8 * 8 * 8)
        for (a in 0 until 8 * 8 * 8) {
            chunksSizes[a] = dis.readInt()
        }

        // Load in the compressed chunks
        for (a in 0..7)
            for (b in 0..7)
                for (c in 0..7) {
                    val expectedSize = chunksSizes[a * 8 * 8 + b * 8 + c]
                    if (expectedSize > 0) {
                        region.getChunkHolder(a, b, c).compressedData = ChunkCompressedData.fromBytes(dis)
                    }
                }
    }

    fun writeHeader(dos: DataOutputStream) {
        // Write the 16-byte header
        dos.writeLong(6003953969960732739L)
        dos.writeInt(0x2E)
        dos.writeLong(System.currentTimeMillis())
    }

    fun writeContents(dos: DataOutputStream, region: RegionImplementation) {
        val dataPerChunk =
                (0..7).map { x ->
                    (0..7).map { y ->
                        (0..7).map { z ->
                            val chunkHolder = region.getChunkHolder(x, y, z)
                            val compressedChunkData = chunkHolder.compressedData
                            if (compressedChunkData != null)
                                captureOutputData { compressedChunkData.toBytes(it) }
                            else
                                null
                        }.toTypedArray()
                    }.toTypedArray()
                }.toTypedArray()

        // we write the index header
        for (a in 0..7)
            for (b in 0..7)
                for (c in 0..7) {
                    val byteArray = dataPerChunk[a][b][c]
                    if (byteArray != null)
                        dos.writeInt(byteArray.size)
                    else
                        dos.writeInt(0) // Ungenerated chunk
                }

        // Then write the relevant worldInfo where it exists
        for (a in 0..7)
            for (b in 0..7)
                for (c in 0..7) {
                    val byteArray = dataPerChunk[a][b][c]
                    if (byteArray != null) {
                        dos.write(byteArray)
                    }
                }

    }

    private val logger = LoggerFactory.getLogger("world.region.serdes")
}

fun captureOutputData(producer: (DataOutputStream) -> Unit): ByteArray {
    val baos = ByteArrayOutputStream()
    val dos = DataOutputStream(baos)

    producer(dos)

    dos.close()
    baos.close()
    return baos.toByteArray()
}