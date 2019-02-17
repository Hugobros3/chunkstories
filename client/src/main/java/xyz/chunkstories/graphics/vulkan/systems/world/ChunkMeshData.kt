package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.resources.RefCountedProperty
import xyz.chunkstories.graphics.vulkan.resources.RefCountedRecyclable

class ChunkMeshData(
        val vertexBuffer: VulkanVertexBuffer?,
        val count: Int, property: RefCountedProperty<*>
) : RefCountedRecyclable(property), Representation {

    override fun cleanup() {
        vertexBuffer?.cleanup()
    }
}