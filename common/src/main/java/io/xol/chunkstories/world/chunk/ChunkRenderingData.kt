package io.xol.chunkstories.world.chunk

import io.xol.chunkstories.api.GameContext
import io.xol.chunkstories.api.util.concurrency.Fence
import io.xol.chunkstories.api.workers.TaskExecutor
import io.xol.chunkstories.api.world.chunk.Chunk
import io.xol.chunkstories.util.concurrency.TrivialFence
import io.xol.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

object DummyChunkRenderingData : Chunk.ChunkMesh {
    override fun requestUpdateAndGetFence() = TrivialFence()
    override fun requestUpdate() = Unit
}