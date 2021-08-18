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

    fun createInstructions(ingameClient: IngameClient): RenderGraphDeclaration.() -> Unit = renderGraph {
        if (backend is VulkanGraphicsBackend && ingameClient.engine.configuration.getBooleanValue(VulkanBackendOptions.raytracedGI))
            createWorldRaytracingRenderGraph(ingameClient, backend as VulkanGraphicsBackend, world)()
        else
            createWorldDeferredRenderGraph(ingameClient, backend, world)()
    }

}