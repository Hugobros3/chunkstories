package xyz.chunkstories.graphics.vulkan.systems.world.farterrain

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.graphics.systems.drawing.FarTerrainDrawer
import xyz.chunkstories.api.graphics.systems.drawing.FullscreenQuadDrawer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.graphics.vulkan.shaders.VulkanShaderProgram
import xyz.chunkstories.graphics.vulkan.shaders.bindShaderResources
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
import xyz.chunkstories.graphics.vulkan.util.VkBuffer
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration
import java.nio.ByteBuffer

class VulkanFarTerrainRenderer(pass: VulkanPass, dslCode: VulkanFarTerrainRenderer.() -> Unit) : FarTerrainDrawer, VulkanDrawingSystem(pass) {
    val backend: VulkanGraphicsBackend
        get() = pass.backend
    val client: IngameClient
        get() = backend.window.client.ingame!!

    val maxPatches = 4096
    val drawBufferSize: Long = 4L * 4 * maxPatches
    private val vkBuffers = InflightFrameResource<VulkanBuffer>(backend) {
        VulkanBuffer(backend, drawBufferSize, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT, MemoryUsagePattern.DYNAMIC)
    }
    private val uploadBuffer = memAlloc(drawBufferSize.toInt())

    private val program: VulkanShaderProgram
    private val pipeline: Pipeline

    init {
        dslCode()

        val shaderName = "farterrain"

        program = backend.shaderFactory.createProgram(shaderName)
        pipeline = Pipeline(backend, program, pass, vertexInputConfiguration { /** nothing hahahaha */ }, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)
    }

    override fun registerDrawingCommands(frame: VulkanFrame, ctx: SystemExecutionContext, commandBuffer: VkCommandBuffer) {
        val bindingContext = backend.descriptorMegapool.getBindingContext(pipeline)
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
        ctx.bindShaderResources(bindingContext)

        uploadBuffer.clear()
        uploadBuffer.putFloat(0.0f)
        uploadBuffer.putFloat(0.0f)
        uploadBuffer.putFloat(100.0f)
        uploadBuffer.putInt(16)

        val vkBuffer = vkBuffers[frame]
        vkBuffer.upload(uploadBuffer)

        bindingContext.bindSSBO("elementsBuffer", vkBuffer)
        //vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffer.handle), MemoryStack.stackLongs(0))
        bindingContext.preDraw(commandBuffer)
        vkCmdDraw(commandBuffer, 3 * 16 * 16, 1, 0, 0)

        frame.recyclingTasks.add {
            bindingContext.recycle()
        }
    }

    override fun cleanup() {
        pipeline.cleanup()
        program.cleanup()

        memFree(uploadBuffer)
    }
}