package xyz.chunkstories.graphics.vulkan.world

import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.common.world.EntitiesRepresentationsProvider
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.systems.dispatching.chunks.VulkanChunkRepresentationsProvider
import xyz.chunkstories.world.WorldImplementation

class VulkanWorldRenderer(override val backend: VulkanGraphicsBackend, world: WorldImplementation) : WorldRenderer(world) {
    val chunksRepresentationsProvider = VulkanChunkRepresentationsProvider(backend, world)
    val entitiesProvider = EntitiesRepresentationsProvider(world)

    // val client = world.gameInstance.engine as Client

    init {
        backend.graphicsEngine.loadRenderGraph(createInstructions(world.gameInstance as IngameClient))
        backend.graphicsEngine.representationsProviders.registerProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.registerProvider(entitiesProvider)
    }

    override fun cleanup() {
        backend.graphicsEngine.representationsProviders.unregisterProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.unregisterProvider(entitiesProvider)
    }
}