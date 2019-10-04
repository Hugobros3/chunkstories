package xyz.chunkstories.graphics.vulkan.systems.world

import xyz.chunkstories.graphics.common.world.ChunkRepresentation
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.resources.RefCountedProperty
import xyz.chunkstories.graphics.vulkan.resources.RefCountedRecyclable
import xyz.chunkstories.world.chunk.ChunkImplementation

class VulkanChunkRepresentation constructor(override val chunk: ChunkImplementation, val sections: Map<String, Section>, property: RefCountedProperty<*>
) : RefCountedRecyclable(property), ChunkRepresentation {

    class Section(val materialTag: String, val cubes: CubesInstances?, val staticMesh: StaticMesh?) {
        lateinit var parent: VulkanChunkRepresentation

        data class StaticMesh(val buffer: VulkanVertexBuffer, val count: Int) {
            lateinit var parent: VulkanChunkRepresentation
        }

        data class CubesInstances(val buffer: VulkanVertexBuffer, val count: Int) {
            lateinit var parent: VulkanChunkRepresentation
        }
    }

    init {
        sections.values.forEach {
            it.parent = this
            it.cubes?.parent = this
            it.staticMesh?.parent = this
        }
    }

    override fun cleanup() {
        sections.values.forEach {
            it.cubes?.buffer?.cleanup()
            it.staticMesh?.buffer?.cleanup()
        }
    }
}