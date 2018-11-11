package io.xol.chunkstories.world.chunk

import io.xol.chunkstories.api.world.chunk.Chunk

open class ChunkRenderingData(val chunk: CubicChunk) : Chunk.ChunkMesh {
    override fun pendingUpdates(): Int {
        return 0
    }

    override fun incrementPendingUpdates() {

    }

    open fun destroy() = Unit
}