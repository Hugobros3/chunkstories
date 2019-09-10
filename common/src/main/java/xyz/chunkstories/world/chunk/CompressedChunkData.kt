package xyz.chunkstories.world.chunk

import xyz.chunkstories.api.content.json.Json

data class CompressedChunkData(val voxelData: ByteArray?, val voxelExtendedData: Json.Array?, val entities: Json.Array?)