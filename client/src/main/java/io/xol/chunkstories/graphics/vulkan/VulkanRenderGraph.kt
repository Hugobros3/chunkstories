package io.xol.chunkstories.graphics.vulkan

import io.xol.chunkstories.api.dsl.*
import io.xol.chunkstories.api.graphics.rendergraph.Pass
import io.xol.chunkstories.api.graphics.rendergraph.RegisteredDrawingSystem
import io.xol.chunkstories.api.graphics.rendergraph.RenderBuffer
import io.xol.chunkstories.api.graphics.rendergraph.RenderGraph
import io.xol.chunkstories.api.graphics.systems.drawing.DrawingSystem
import io.xol.chunkstories.graphics.vulkan.resources.Cleanable
import io.xol.chunkstories.graphics.vulkan.systems.VulkanPass
import io.xol.chunkstories.graphics.vulkan.textures.VulkanRenderBuffer
import org.joml.Vector2i
import org.lwjgl.vulkan.VK10
import kotlin.reflect.KClass

class VulkanRenderGraph(val backend: VulkanGraphicsBackend, script: RenderGraphDeclarationScript) : RenderGraph, Cleanable {

    val commandPool: CommandPool

    override val buffers = mutableMapOf<String, VulkanRenderBuffer>()
    override val passes = mutableMapOf<String, VulkanPass>()

    override lateinit var defaultPass: Pass
    override lateinit var finalPass: Pass

    override val viewportSize: Vector2i
            get() = Vector2i(backend.window.width, backend.window.height)

    val parser = object : RenderGraphDeclarationsContext {
        override val renderGraph = this@VulkanRenderGraph

        /** Enter the context to declare a bunch of RenderBuffers */
        override fun renderBuffers(renderBufferDeclarations: RenderBuffersDeclarationCtx.() -> Unit) = object : RenderBuffersDeclarationCtx {

            /** Declare a render buffer and add it to the graph */
            override fun renderBuffer(renderBufferConfiguration: RenderBuffer.() -> Unit) {
                val renderBuffer = VulkanRenderBuffer(backend, this@VulkanRenderGraph, renderBufferConfiguration)
                buffers[renderBuffer.name] = renderBuffer
            }
        }.apply(renderBufferDeclarations)

        /** Enter the context to declare a bunch of Passes */
        override fun passes(function: PassesDeclarationCtx.() -> Unit) = object : PassesDeclarationCtx {
            /** Declare a pass and add it to the graph */
            override fun pass(config: Pass.() -> Unit) {
                val pass = VulkanPass(backend, this@VulkanRenderGraph, config)
                passes[pass.name] = pass
            }

            override fun Pass.draws(drawsDeclarations: DrawsDeclarationCtx.() -> Unit) { object : DrawsDeclarationCtx {
                override fun <T : DrawingSystem> system(systemClass: KClass<T>, systemConfiguration: T.() -> Unit) {
                    this@draws.declaredDrawingSystems.add(RegisteredDrawingSystem(systemClass.java, systemConfiguration as DrawingSystem.() -> Unit))
                }

            }.apply(drawsDeclarations)}

        }.apply(function)
    }

    init {
        commandPool = CommandPool(backend, backend.logicalDevice.graphicsQueue.family, VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)

        parser.apply(script)

        defaultPass = passes.values.find { it.default } ?: throw Exception("No default pass was set !")
        finalPass = passes.values.find { it.final } ?: throw Exception("No final pass was set !")
    }

    override fun cleanup() {
        commandPool.cleanup()
    }
}