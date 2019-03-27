package xyz.chunkstories.world.chunk

import xyz.chunkstories.api.GameContext
import xyz.chunkstories.api.util.concurrency.Fence
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.chunk.ChunkMesh
import xyz.chunkstories.util.concurrency.TrivialFence
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

object DummyChunkRenderingData : ChunkMesh {
    override fun requestUpdateAndGetFence() = TrivialFence()
    override fun requestUpdate() = Unit
}