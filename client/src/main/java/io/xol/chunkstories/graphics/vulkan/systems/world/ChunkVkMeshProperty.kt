package io.xol.chunkstories.graphics.vulkan.systems.world

import io.xol.chunkstories.api.world.chunk.Chunk
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.world.chunk.CubicChunk
import io.xol.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

class ChunkVkMeshProperty(val backend: VulkanGraphicsBackend, val chunk: CubicChunk) : AutoRebuildingProperty(chunk.world.gameContext, true), Chunk.ChunkMesh {
    val property = RefCountedProperty<ChunkVulkanMeshData>()

    init {
        requestUpdate()
    }

    fun get(): ChunkVulkanMeshData? {
        try {
            lock.lock()
            val value = property.get()
            if (value == null && task == null)
                this.requestUpdate()
            return value
        } finally {
            lock.unlock()
        }
    }

    fun acceptNewData(vertexBuffer: VulkanVertexBuffer?, count: Int) {
        val data = ChunkVulkanMeshData(vertexBuffer, count, property)
        property.set(data)
    }

    inner class ChunkVulkanMeshData(val vertexBuffer: VulkanVertexBuffer?, val count: Int, property: RefCountedProperty<*>) : RefCountedRecyclable(property) {
        override fun cleanup() {
            vertexBuffer?.cleanup()
        }
    }

    override fun createTask(updatesToConsider: Int): UpdateTask = TaskCreateChunkMesh(backend, chunk, this, updatesToConsider)

    override fun cleanup() {
        task?.tryCancel()
        property.data?.release()
    }
}