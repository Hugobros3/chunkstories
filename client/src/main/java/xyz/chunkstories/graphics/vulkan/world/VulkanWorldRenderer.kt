package xyz.chunkstories.graphics.vulkan.world

import xyz.chunkstories.graphics.common.DefaultIngameRendergraph
import xyz.chunkstories.graphics.common.WorldRenderer
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.systems.world.ChunkRepresentationsProvider
import xyz.chunkstories.graphics.vulkan.world.entities.EntitiesRepresentationsProvider
import xyz.chunkstories.world.WorldClientCommon

class VulkanWorldRenderer(val backend: VulkanGraphicsBackend, world: WorldClientCommon) : WorldRenderer(world) {
    val chunksRepresentationsProvider = ChunkRepresentationsProvider(backend, world)
    val entitiesProvider = EntitiesRepresentationsProvider(world)

    init {
        backend.graphicsEngine.loadRenderGraph(DefaultIngameRendergraph.instructions)

        backend.graphicsEngine.representationsProviders.registerProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.registerProvider(entitiesProvider)
    }

    override fun cleanup() {
        backend.graphicsEngine.representationsProviders.unregisterProvider(chunksRepresentationsProvider)
        backend.graphicsEngine.representationsProviders.unregisterProvider(entitiesProvider)
        //chunksRepresentationsProvider.cleanup()
    }

}