package xyz.chunkstories.world.chunk

import net.jpountz.lz4.LZ4Factory
import org.lwjgl.system.MemoryUtil
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asArray
import xyz.chunkstories.api.content.json.stringSerialize
import xyz.chunkstories.api.content.json.toJson
import xyz.chunkstories.api.entity.EntitySerialization
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import net.jpountz.lz4.LZ4Exception
import xyz.chunkstories.api.entity.traits.TraitDontSave


sealed class ChunkCompressedData(internal val entities: Json.Array ) {

    class Air(entities: Json.Array) : ChunkCompressedData(entities)

    class NonAir constructor(internal val voxelData: ByteArray, internal val voxelExtendedData: Json.Array, entities: Json.Array) : ChunkCompressedData(entities) {
        fun extractVoxelData() : IntArray {
            val f4st = MemoryUtil.memAlloc(voxelData.size)
            f4st.put(voxelData)
            f4st.flip()

            val t3mp = MemoryUtil.memAlloc(32 * 32 * 32 * 4)
            try {
                lz4.get().fastDecompressor().decompress(f4st, t3mp)

                t3mp.flip()

                MemoryUtil.memFree(f4st)

                val data = IntArray(32 * 32 * 32)
                t3mp.asIntBuffer().get(data)

                MemoryUtil.memFree(t3mp)

                return data
            } catch (e: LZ4Exception) {
                throw UnloadableChunkDataException("LZ4 decompression failed.")
            }
        }

        fun extractVoxelExtendedData() : Json.Array {
            return voxelExtendedData
        }
    }

    fun extractEntities() : Json.Array {
        return entities
    }

    companion object {
        val lz4 = ThreadLocal.withInitial {
            LZ4Factory.fastestInstance()
        }

        private fun compressVoxelData(voxelDataArray: IntArray): ByteArray {
            val uncompressedByteBuffer = MemoryUtil.memAlloc(32 * 32 * 32 * 4)
            uncompressedByteBuffer.asIntBuffer().put(voxelDataArray)

            // Allow for some potential compression overhead (pessimistic worst case)
            val compressedByteBuffer = MemoryUtil.memAlloc(32 * 32 * 32 * 4 + 512)

            val compressor = lz4.get().fastCompressor()
            compressor.compress(uncompressedByteBuffer, compressedByteBuffer)

            MemoryUtil.memFree(uncompressedByteBuffer)

            val compressed = ByteArray(compressedByteBuffer.position())
            compressedByteBuffer.flip()
            compressedByteBuffer.get(compressed)

            MemoryUtil.memFree(compressedByteBuffer)
            return compressed
        }

        fun compressChunkData(chunk: ChunkImplementation): ChunkCompressedData {
            val compressedEntityData = Json.Array(chunk.entitiesWithinChunk.filter { it.traits[TraitDontSave::class] == null }.map { EntitySerialization.serializeEntity(it) })

            val compressedVoxelData = chunk.voxelDataArray?.let{ compressVoxelData(it) } ?: return Air(compressedEntityData)

            val compressedExtendedData = Json.Array(chunk.allCellComponents.values.map { Json.Dict(mapOf(
                    "index" to Json.Value.Number(it.index.toDouble()),
                    "components" to Json.Array(it.map.mapNotNull { cellComponentEntry ->
                        val serialized = cellComponentEntry.value.serialize() ?: return@mapNotNull null
                        Json.Dict(mapOf(
                            "name" to Json.Value.Text(cellComponentEntry.key),
                            "data" to serialized
                    )) })
            )) })

            return NonAir(compressedVoxelData, compressedExtendedData, compressedEntityData)
        }


        fun fromBytes(dis: DataInputStream): ChunkCompressedData {
            val type = dis.read()
            if(type == 0) {
                return Air(dis.readUTF().toJson().asArray!!)
            } else {
                val compressedVoxelDataSize = dis.readInt()
                val compressedVoxelData = ByteArray(compressedVoxelDataSize)
                dis.readFully(compressedVoxelData)

                return NonAir(compressedVoxelData, dis.readUTF().toJson().asArray!!,  dis.readUTF().toJson().asArray!!)
            }
        }
    }

    fun toBytes(dos: DataOutputStream) {
        if(this is Air)
            dos.write(0)
        else
            dos.write(1)

        if(this is NonAir) {
            dos.writeInt(this.voxelData.size)
            dos.write(this.voxelData)

            dos.writeUTF(this.voxelExtendedData.stringSerialize())
        }

        dos.writeUTF(this.entities.stringSerialize())
    }

    /** Networking sends entities in a different way */
    fun stripEntities(): ChunkCompressedData =
        when(this) {
            is Air -> Air(Json.Array(emptyList()))
            is NonAir -> NonAir(voxelData, voxelExtendedData, Json.Array(emptyList()))
        }
}