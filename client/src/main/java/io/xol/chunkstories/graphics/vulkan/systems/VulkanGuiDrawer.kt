package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.graphics.common.DummyGuiDrawer
import io.xol.chunkstories.graphics.vulkan.*
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.shaders.UniformTestOffset
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.resources.InflightFrameResource
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

    val guiShaderProgram = backend.shaderFactory.createProgram(backend, "/shaders/gui/gui")

    val pipeline = Pipeline(backend, pass.renderPass, guiShaderProgram) {
        val bindingDescription = VkVertexInputBindingDescription.callocStack(1).apply {
            binding(0)
            stride(2 * 4 + 2 * 4 + 4 * 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        val attributeDescriptions = VkVertexInputAttributeDescription.callocStack(3)
        attributeDescriptions[0].apply {
            binding(0)
            location(guiShaderProgram.glslProgram.vertexInputs.find { it.name == "vertexIn" }?.location!! )
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(0)
        }

        attributeDescriptions[1].apply {
            binding(0)
            location(guiShaderProgram.glslProgram.vertexInputs.find { it.name == "texCoordIn" }?.location!! )
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(2 * 4)
        }

        attributeDescriptions[2].apply {
            binding(0)
            location(guiShaderProgram.glslProgram.vertexInputs.find { it.name == "colorIn" }?.location!! )
            format(VK_FORMAT_R32G32B32A32_SFLOAT)
            offset(2 * 4 + 2 * 4)
        }

        VkPipelineVertexInputStateCreateInfo.callocStack().sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO).apply {
            pVertexBindingDescriptions(bindingDescription)
            pVertexAttributeDescriptions(attributeDescriptions)
        }
    }

    val descriptorPool = DescriptorPool(backend, guiShaderProgram)

    val sampler = VulkanSampler(backend)

    val vertexBuffers: InflightFrameResource<VulkanVertexBuffer>

    init {

        stackPush()

        vertexBuffers = InflightFrameResource(backend) {
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

            fun texCoord(a: Float, b: Float) {
                stagingFloatBuffer.put(a)
                stagingFloatBuffer.put(b)
            }

            fun color() {
                stagingFloatBuffer.put(color?.x() ?: 1.0F)
                stagingFloatBuffer.put(color?.y() ?: 1.0F)
                stagingFloatBuffer.put(color?.z() ?: 1.0F)
                stagingFloatBuffer.put(color?.w() ?: 1.0F)
            }
            
            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color()
            vertex((startX), (startY + height))
            texCoord(textureStartX, textureEndY)
            color()
            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color()

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color()
            vertex((startX + width), (startY))
            texCoord(textureEndX, textureStartY)
            color()
            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color()

            stagingSize += 2
        }

        override fun drawBoxWithCorners(posx: Int, posy: Int, width: Int, height: Int, cornerSizeDivider: Int, texture: String) {
            drawBox(posx, posy, width, height, Vector4f(1.0F))
        }

        override fun drawString(font: Font, xPosition: Int, yPosition: Int, text: String, color: Vector4fc) {
            println(text)
        }
    }

    override fun registerDrawingCommands(frame : Frame, commandBuffer: VkCommandBuffer) {
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

            // Write the commands in the command buffer
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 0, descriptorPool.setsForFrame(frame), null as? IntArray)
            vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffers[frame].handle), MemoryStack.stackLongs(0))
            vkCmdDraw(commandBuffer, 3 * stagingSize, 1, 0, 0) // that's rather anticlimactic
        }
    }

    override fun cleanup() {
        sampler.cleanup()
        vertexBuffers.cleanup()

        pipeline.cleanup()

        guiShaderProgram.cleanup()

        descriptorPool.cleanup()

        MemoryUtil.memFree(stagingByteBuffer)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan.triangleTest")
    }

}