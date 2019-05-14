package xyz.chunkstories.graphics.common.world

import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.world.chunk.CubicChunk

interface ChunkRepresentation : Representation {
    val chunk: CubicChunk
}