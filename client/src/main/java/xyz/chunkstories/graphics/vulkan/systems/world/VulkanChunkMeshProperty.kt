package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.RefCountedProperty
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

class VulkanChunkMeshProperty(val backend: VulkanGraphicsBackend, val chunk: CubicChunk) : AutoRebuildingProperty(chunk.world.gameContext, true), Chunk.ChunkMesh {
    val actualProperty = RefCountedProperty<ChunkRepresentation>()

    init {
        //requestUpdate()
    }

    fun get(): ChunkRepresentation? {
        try {
            lock.lock()
            val value = actualProperty.get()
            if (value == null && task == null)
                this.requestUpdate()
            return value
        } finally {
            lock.unlock()
        }
    }

    fun acceptNewData(sections: Map<String, ChunkRepresentation.Section>) {
        val data = ChunkRepresentation(chunk, sections, actualProperty)
        actualProperty.set(data)
    }

    override fun createTask(updatesToConsider: Int): UpdateTask = TaskCreateChunkMesh(backend, chunk, this, updatesToConsider)

    override fun cleanup() {
        actualProperty.data?.release()
    }
}