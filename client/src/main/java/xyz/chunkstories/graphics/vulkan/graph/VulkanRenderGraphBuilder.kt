package xyz.chunkstories.graphics.vulkan.graph

import xyz.chunkstories.api.dsl.*
import xyz.chunkstories.api.graphics.rendergraph.Pass
import xyz.chunkstories.api.graphics.rendergraph.PassOutput
import xyz.chunkstories.api.graphics.rendergraph.RegisteredDrawingSystem
import xyz.chunkstories.api.graphics.rendergraph.RenderBuffer
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import kotlin.reflect.KClass

/** Implements the interfaces of the declaration context and links them to the creation of actual Passes and RenderBuffers objects */
internal class VulkanRenderGraphBuilder(override val renderGraph: VulkanRenderGraph) : RenderGraphDeclarationsContext {

    /** Enter the context to declare a bunch of RenderBuffers */
    override fun renderBuffers(renderBufferDeclarations: RenderBuffersDeclarationCtx.() -> Unit) = object : RenderBuffersDeclarationCtx {

        /** Declare a render buffer and add it to the graph */
        override fun renderBuffer(renderBufferConfiguration: RenderBuffer.() -> Unit) {
            val renderBuffer = VulkanRenderBuffer(renderGraph.backend, renderGraph, renderBufferConfiguration)
            renderGraph.buffers[renderBuffer.name] = renderBuffer
        }
    }.apply(renderBufferDeclarations)

    /** Enter the context to declare a bunch of Passes */
    override fun passes(function: PassesDeclarationCtx.() -> Unit) = object : PassesDeclarationCtx {
        /** Declare a pass and add it to the graph */
        override fun pass(config: Pass.() -> Unit) {
            val pass = VulkanPass(renderGraph.backend, renderGraph, config)
            renderGraph.passes[pass.name] = pass
        }

        override fun Pass.outputs(outputsDeclarations: PassOutputsDeclarationCtx.() -> Unit): PassOutputsDeclarationCtx {
            return object : PassOutputsDeclarationCtx {
                override fun output(outputConfiguration: PassOutput.() -> Unit) {
                    val output = PassOutput().apply(outputConfiguration)
                    this@outputs.outputs.add(output)
                }

            }.apply(outputsDeclarations)
        }

        override fun Pass.draws(drawsDeclarations: DrawsDeclarationCtx.() -> Unit) {
            object : DrawsDeclarationCtx {
                override fun <T : DrawingSystem> system(systemClass: KClass<T>, systemConfiguration: T.() -> Unit) {
                    this@draws.declaredDrawingSystems.add(RegisteredDrawingSystem(systemClass.java, systemConfiguration as DrawingSystem.() -> Unit))
                }

            }.apply(drawsDeclarations)
        }

    }.apply(function)
}