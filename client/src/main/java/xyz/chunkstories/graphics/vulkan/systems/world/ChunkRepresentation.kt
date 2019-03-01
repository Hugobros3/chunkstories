package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.api.graphics.representation.Representation
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.resources.RefCountedProperty
import xyz.chunkstories.graphics.vulkan.resources.RefCountedRecyclable
import xyz.chunkstories.world.chunk.CubicChunk

class ChunkRepresentation(val chunk: CubicChunk, val sections: Map<String, Section>, property: RefCountedProperty<*>
) : RefCountedRecyclable(property), Representation {

    class Section(val name: String, val buffer: VulkanVertexBuffer, val count: Int)

    override fun cleanup() {
        sections.values.forEach {
            it.buffer.cleanup()
        }
    }
}