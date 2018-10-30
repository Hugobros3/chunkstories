package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.graphics.common.DummyGuiDrawer
import io.xol.chunkstories.graphics.vulkan.*
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.shaders.UniformTestOffset
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.resources.PerFrameResource
import io.xol.chunkstories.graphics.vulkan.textures.VulkanSampler
import io.xol.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import io.xol.chunkstories.gui.ClientGui
import org.joml.Vector4f
import org.joml.Vector4fc
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.slf4j.LoggerFactory

internal const val guiBufferSize = 16384

class VulkanGuiDrawer(pass: VulkanPass, val gui: ClientGui) : VulkanDrawingSystem(pass) {

    val backend: VulkanGraphicsBackend
        get() = pass.backend

    val baseProgram = backend.shaderFactory.createProgram(backend, "/shaders/gui/gui")
    val pipeline = Pipeline(backend, backend.renderToBackbuffer.handle, baseProgram)
    val descriptorPool = DescriptorPool(backend, baseProgram)

    val sampler = VulkanSampler(backend)

    val vertexBuffers: PerFrameResource<VulkanVertexBuffer>

    init {

        stackPush()

        vertexBuffers = PerFrameResource(backend) {
            VulkanVertexBuffer(backend, guiBufferSize.toLong())
        }

        stackPop()
    }

    /** Accumulation for GUI contents */
    val stagingByteBuffer = MemoryUtil.memAlloc(guiBufferSize)
    val stagingFloatBuffer = stagingByteBuffer.asFloatBuffer()
    var stagingSize = 0

    val drawer = object : DummyGuiDrawer(gui) {
        override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: String?, color: Vector4fc?) {
            val sx = 1.0F / gui.viewportWidth.toFloat()
            val sy = 1.0F / gui.viewportHeight.toFloat()

            fun vertex(a: Int, b: Int) {
                stagingFloatBuffer.put(-1.0F + 2.0F * (a * sx))
                stagingFloatBuffer.put(1.0F - 2.0F * (b * sy))
            }
            
            vertex((startX), startY)
            vertex((startX), (startY + height))
            vertex((startX + width), (startY + height))

            vertex((startX), startY)
            vertex((startX + width), (startY))
            vertex((startX + width), (startY + height))

            stagingSize += 2
        }

        override fun drawBoxWithCorners(posx: Int, posy: Int, width: Int, height: Int, cornerSizeDivider: Int, texture: String) {
            drawBox(posx, posy, width, height, Vector4f(1.0F))
        }

        override fun drawString(font: Font, xPosition: Int, yPosition: Int, text: String, color: Vector4fc) {
            println(text)
        }
    }

    override fun render(frame : Frame, commandBuffer: VkCommandBuffer) {
        stackPush().use {
            stagingByteBuffer.clear()
            stagingFloatBuffer.clear()
            stagingSize = 0

            gui.topLayer?.render(drawer)

            // Rewrite the vertex buffer
            vertexBuffers[frame].apply {
                this.upload(stagingByteBuffer)
            }

            val testOffset = UniformTestOffset()
            testOffset.offset.x = (Math.random().toFloat() - 0.5F) * 0.2F

            //descriptorPool.configure(frame, testOffset)
            descriptorPool.configureTextureAndSampler(frame, "diffuseTexture", backend.textures.defaultTexture2D as VulkanTexture2D, sampler)

            // Rewrite the command buffer
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 0, descriptorPool.setsForFrame(frame), null as? IntArray)
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffers[frame].handle), MemoryStack.stackLongs(0))
            vkCmdDraw(commandBuffer, 3 * stagingSize, 1, 0, 0) // that's rather anticlimactic
        }
    }

    override fun cleanup() {

        sampler.cleanup()
        vertexBuffers.cleanup()

        pipeline.cleanup()

        baseProgram.cleanup()

        descriptorPool.cleanup()

        MemoryUtil.memFree(stagingByteBuffer)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan.triangleTest")
    }

}