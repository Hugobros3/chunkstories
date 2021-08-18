package xyz.chunkstories.world.chunk.deriveddata

import xyz.chunkstories.EngineImplemI
import xyz.chunkstories.api.workers.TaskExecutor
import xyz.chunkstories.api.world.chunk.ChunkOcclusionManager
import xyz.chunkstories.world.chunk.ChunkImplementation

class ChunkOcclusionProperty(val chunk: ChunkImplementation) : AutoRebuildingProperty(chunk.world.gameInstance.engine.tasks, false), ChunkOcclusionManager {

    override fun createTask(updatesToConsider: Int): UpdateTask = ChunkOcclusionRebuildTask(this, updatesToConsider)

    //TODO canGoFromChunkAToB

    class ChunkOcclusionRebuildTask(attachedProperty: AutoRebuildingProperty, updates: Int) : UpdateTask(attachedProperty, updates) {
        override fun update(taskExecutor: TaskExecutor): Boolean {
            //TODO
            return true
        }

    }
}