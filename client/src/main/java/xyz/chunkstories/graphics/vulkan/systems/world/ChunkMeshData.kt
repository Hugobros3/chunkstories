package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.resources.RefCountedProperty
import xyz.chunkstories.graphics.vulkan.resources.RefCountedRecyclable

class ChunkMeshData(val sections: Map<String, Section>, property: RefCountedProperty<*>
) : RefCountedRecyclable(property), Representation {

    override fun cleanup() {
        sections.values.forEach {
            it.buffer.cleanup()
        }
    }

    class Section(val name: String, val buffer: VulkanVertexBuffer, val count: Int) {
    }
}