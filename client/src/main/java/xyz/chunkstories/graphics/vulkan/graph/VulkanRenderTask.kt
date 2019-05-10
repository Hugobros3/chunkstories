package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.graphics.rendergraph.RenderTaskDeclaration
import xyz.chunkstories.graphics.common.Cleanable
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend

class VulkanRenderTask(val backend: VulkanGraphicsBackend, val renderGraph: VulkanRenderGraph, val declaration: RenderTaskDeclaration) : Cleanable {
    val buffers: Map<String, VulkanRenderBuffer>
    val passes: Map<String, VulkanPass>

    var rootPass: VulkanPass

    init {
        buffers = declaration.renderBuffersDeclarations.renderBuffers.map {
            val vulkanRenderBuffer = VulkanRenderBuffer(backend, it)
            Pair(it.name, vulkanRenderBuffer)
        }.toMap()

        passes = declaration.passesDeclarations.passes.map {
            val vulkanPass = VulkanPass(backend, this@VulkanRenderTask, it)
            Pair(it.name, vulkanPass)
        }.toMap()

        rootPass = passes.values.find { it.declaration.name == declaration.finalPassName }!!
    }

    override fun cleanup() {
        buffers.values.forEach(Cleanable::cleanup)
        passes.values.forEach(Cleanable::cleanup)
    }
}