package xyz.chunkstories.world.chunk

import xyz.chunkstories.api.world.chunk.ChunkMesh
import xyz.chunkstories.util.concurrency.TrivialFence

object DummyChunkRenderingData : ChunkMesh {
    override fun requestUpdateAndGetFence() = TrivialFence()
    override fun requestUpdate() = Unit
}