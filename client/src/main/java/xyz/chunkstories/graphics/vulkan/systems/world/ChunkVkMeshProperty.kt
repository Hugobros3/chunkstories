package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.resources.RefCountedProperty
import xyz.chunkstories.graphics.vulkan.resources.RefCountedRecyclable
import xyz.chunkstories.world.chunk.CubicChunk
import xyz.chunkstories.world.chunk.deriveddata.AutoRebuildingProperty

class ChunkVkMeshProperty(val backend: VulkanGraphicsBackend, val chunk: CubicChunk) : AutoRebuildingProperty(chunk.world.gameContext, true), Chunk.ChunkMesh {
    val actualProperty = RefCountedProperty<ChunkMeshData>()

    init {
        //requestUpdate()
    }

    fun get(): ChunkMeshData? {
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

    fun acceptNewData(vertexBuffer: VulkanVertexBuffer?, count: Int) {
        val data = ChunkMeshData(vertexBuffer, count, actualProperty)
        actualProperty.set(data)
    }

    override fun createTask(updatesToConsider: Int): UpdateTask = TaskCreateChunkMesh(backend, chunk, this, updatesToConsider)

    override fun cleanup() {
        actualProperty.data?.release()
    }
}