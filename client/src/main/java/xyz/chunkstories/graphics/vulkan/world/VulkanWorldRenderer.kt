package xyz.chunkstories.graphics.vulkan.world

import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.common.world.EntitiesRepresentationsProvider
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.systems.world.VulkanChunkRepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class VulkanWorldRenderer(override val backend: VulkanGraphicsBackend, world: WorldClientCommon) : WorldRenderer(world) {
    val chunksRepresentationsProvider = VulkanChunkRepresentationsProvider(backend, world)
    val entitiesProvider = EntitiesRepresentationsProvider(world)

    val client = world.gameContext

    init {
        backend.graphicsEngine.loadRenderGraph(createInstructions(world.client))
        backend.graphicsEngine.representationsProviders.registerProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.registerProvider(entitiesProvider)
    }

    override fun cleanup() {
        backend.graphicsEngine.representationsProviders.unregisterProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.unregisterProvider(entitiesProvider)
    }
}