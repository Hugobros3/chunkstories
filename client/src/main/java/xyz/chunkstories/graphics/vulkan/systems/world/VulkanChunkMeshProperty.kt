package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.world.chunk.ChunkMesh
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.resources.RefCountedProperty
import xyz.chunkstories.world.chunk.ChunkImplementation
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

class VulkanChunkMeshProperty(val backend: VulkanGraphicsBackend, val chunk: ChunkImplementation) : AutoRebuildingProperty(chunk.world.gameContext, true), ChunkMesh {
    val actualProperty = RefCountedProperty<VulkanChunkRepresentation>()

    init {
        //requestUpdate()
    }

    fun getAndAcquire(): VulkanChunkRepresentation? {
        try {
            lock.lock()
            val value = actualProperty.getAndAcquire()
            if (value == null && task == null)
                this.requestUpdate()
            return value
        } finally {
            lock.unlock()
        }
    }

    fun acceptNewData(sections: Map<String, VulkanChunkRepresentation.Section>) {
        val data = VulkanChunkRepresentation(chunk, sections, actualProperty)
        actualProperty.set(data)
    }

    override fun createTask(updatesToConsider: Int): UpdateTask = TaskCreateVulkanChunkRepresentation(backend, chunk, this, updatesToConsider)

    override fun cleanup() {
        actualProperty.data?.release()
    }
}