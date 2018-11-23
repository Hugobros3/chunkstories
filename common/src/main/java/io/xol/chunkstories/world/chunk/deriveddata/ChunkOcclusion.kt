package io.xol.chunkstories.world.chunk.deriveddata

import io.xol.chunkstories.api.workers.TaskExecutor
import io.xol.chunkstories.api.world.chunk.Chunk
import io.xol.chunkstories.world.chunk.CubicChunk

class ChunkOcclusionProperty(val chunk: CubicChunk) : AutoRebuildingProperty(chunk.world.gameContext, false), Chunk.ChunkOcclusionManager {

    override fun createTask(updatesToConsider: Int): UpdateTask = ChunkOcclusionRebuildTask(this, updatesToConsider)

    //TODO canGoFromChunkAToB

    class ChunkOcclusionRebuildTask(attachedProperty: AutoRebuildingProperty, updates: Int) : UpdateTask(attachedProperty, updates) {
        override fun update(taskExecutor: TaskExecutor): Boolean {
            //TODO
            return true
        }

    }
}