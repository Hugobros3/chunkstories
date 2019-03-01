package xyz.chunkstories.graphics.vulkan.world

import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.systems.world.ChunkRepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class VulkanWorldRenderer(val backend: VulkanGraphicsBackend, world: WorldClientCommon) : WorldRenderer(world) {
    val chunksRepresentationsProvider = ChunkRepresentationsProvider(backend, world)

    init {
        backend.graphicsEngine.representationsProviders.registerProvider(chunksRepresentationsProvider)
    }

    override fun cleanup() {
        backend.graphicsEngine.representationsProviders.unregisterProvider(chunksRepresentationsProvider)
        //chunksRepresentationsProvider.cleanup()
    }

}