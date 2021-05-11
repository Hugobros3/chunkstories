package xyz.chunkstories.graphics.common

import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.rendergraph.RenderGraphDeclaration
import xyz.chunkstories.api.graphics.rendergraph.renderGraph
import xyz.chunkstories.graphics.GLFWBasedGraphicsBackend
import xyz.chunkstories.graphics.common.world.createWorldDeferredRenderGraph
import xyz.chunkstories.graphics.vulkan.world.createWorldRaytracingRenderGraph
import xyz.chunkstories.graphics.vulkan.VulkanBackendOptions
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.world.WorldImplementation

abstract class WorldRenderer(val world: WorldImplementation) : Cleanable {
    abstract val backend: GLFWBasedGraphicsBackend

    fun createInstructions(client: IngameClient): RenderGraphDeclaration.() -> Unit = renderGraph {
        if (backend is VulkanGraphicsBackend && client.configuration.getBooleanValue(VulkanBackendOptions.raytracedGI))
            createWorldRaytracingRenderGraph(client, backend as VulkanGraphicsBackend, world)()
        else
            createWorldDeferredRenderGraph(client, backend, world)()
    }

}