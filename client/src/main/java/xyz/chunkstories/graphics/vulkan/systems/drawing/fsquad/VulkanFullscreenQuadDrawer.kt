package xyz.chunkstories.graphics.vulkan.systems.drawing.fsquad

import org.lwjgl.system.MemoryStack.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.common.shaders.compiler.ShaderCompilationParameters
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.graph.VulkanPassInstance
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.systems.drawing.VulkanDrawingSystem
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration

class VulkanFullscreenQuadDrawer(pass: VulkanPass, dslCode: FullscreenQuadDrawer.() -> Unit) : VulkanDrawingSystem(pass), FullscreenQuadDrawer {
    val backend: VulkanGraphicsBackend
        get() = pass.backend
    val client: IngameClient
        get() = backend.window.client.ingame!!

    override val defines = mutableMapOf<String, String>()

    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding(0)
            stride(2 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }?.location!!)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(0)
        }
    }

    override var shader = pass.declaration.name

    private val program: VulkanShaderProgram
    private val pipeline: Pipeline

    private val vertexBuffer: VulkanVertexBuffer

    init {
        dslCode()

        program = backend.shaderFactory.createProgram(shader, ShaderCompilationParameters(defines = defines))
        pipeline = Pipeline(backend, program, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)

        val vertices = floatArrayOf(-1.0F, -3.0F, 3.0F, 1.0F, -1.0F, 1.0F)
        vertexBuffer = VulkanVertexBuffer(backend, vertices.size * 4L, MemoryUsagePattern.STATIC)

        stackPush().use {
            val byteBuffer = stackMalloc(vertices.size * 4)
            vertices.forEach { f -> byteBuffer.putFloat(f) }
            byteBuffer.flip()

            vertexBuffer.upload(byteBuffer)
        }
    }

    override fun registerDrawingCommands(context: VulkanPassInstance, commandBuffer: VkCommandBuffer) {
        val bindingContext = context.getBindingContext(pipeline)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)

        vkCmdBindVertexBuffers(commandBuffer, 0, stackLongs(vertexBuffer.handle), stackLongs(0))
        bindingContext.commitAndBind(commandBuffer)
        vkCmdDraw(commandBuffer, 3 * 1, 1, 0, 0)

        context.frame.recyclingTasks.add {
            bindingContext.recycle()
        }
    }

    override fun cleanup() {
        vertexBuffer.cleanup()
        pipeline.cleanup()
        program.cleanup()
    }
}