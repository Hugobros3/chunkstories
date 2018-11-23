package io.xol.chunkstories.world.chunk

import io.xol.chunkstories.api.util.concurrency.Fence
import io.xol.chunkstories.api.world.chunk.Chunk
import io.xol.chunkstories.util.concurrency.TrivialFence

open class ChunkRenderingData(val chunk: CubicChunk) : Chunk.ChunkMesh {
    override fun requestUpdate() {
        //Does nothing
    }

    //Does nothing
    override fun requestUpdateAndGetFence(): Fence = TrivialFence()

    open fun destroy() = Unit
}