package io.xol.chunkstories.graphics.vulkan.systems

import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.graphics.common.DummyGuiDrawer
import io.xol.chunkstories.graphics.vulkan.DescriptorPool
import io.xol.chunkstories.graphics.vulkan.Pipeline
import io.xol.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import io.xol.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import io.xol.chunkstories.graphics.vulkan.graph.VulkanPass
import io.xol.chunkstories.graphics.vulkan.resources.InflightFrameResource
import io.xol.chunkstories.graphics.vulkan.swapchain.Frame
import io.xol.chunkstories.graphics.vulkan.textures.VirtualTexturingHelper
import io.xol.chunkstories.graphics.vulkan.textures.VulkanSampler
import io.xol.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import io.xol.chunkstories.graphics.vulkan.vertexInputConfiguration
import io.xol.chunkstories.gui.ClientGui
import org.joml.Vector4fc
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo
import org.lwjgl.vulkan.VkVertexInputAttributeDescription
import org.lwjgl.vulkan.VkVertexInputBindingDescription
import org.slf4j.LoggerFactory

internal const val guiBufferSize = 16384 * 32

abstract class InternalGuiDrawer(gui: Gui) : DummyGuiDrawer(gui) {
    abstract fun drawQuad(startX: Float, startY: Float, width: Float, height: Float, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: VulkanTexture2D, color: Vector4fc?)
}

class VulkanGuiDrawer(pass: VulkanPass, val gui: ClientGui) : VulkanDrawingSystem(pass) {

    val backend: VulkanGraphicsBackend
        get() = pass.backend

    val fontRenderer = VulkanFontRenderer(backend)
    //val guiShaderProgram = backend.shaderFactory.createProgram(backend, "/shaders/gui/gui")
    
    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding(0)
            stride(2 * 4 + 2 * 4 + 4 * 4 + 4)
            inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "vertexIn" }?.location!!)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(0)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "texCoordIn" }?.location!!)
            format(VK_FORMAT_R32G32_SFLOAT)
            offset(2 * 4)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "colorIn" }?.location!!)
            format(VK_FORMAT_R32G32B32A32_SFLOAT)
            offset(2 * 4 + 2 * 4)
        }

        attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "textureIdIn" }?.location!!)
            format(VK_FORMAT_R32_SINT)
            offset(2 * 4 + 2 * 4 + 4 * 4)
        }
    }

        val pipeline = Pipeline(backend, pass, vertexInputConfiguration)

    val descriptorPool = DescriptorPool(backend, pass.program)
    val sampler = VulkanSampler(backend)

    val vertexBuffers: InflightFrameResource<VulkanVertexBuffer>

    init {
        vertexBuffers = InflightFrameResource(backend) {
            VulkanVertexBuffer(backend, guiBufferSize.toLong())
        }
    }

    val virtualTexturing = VirtualTexturingHelper(backend, pass.program)

    //TODO this is hacky af, fix this plz
    lateinit var virtualTexturingContext: VirtualTexturingHelper.VirtualTexturingContext
    lateinit var commandBuffer: VkCommandBuffer

    /** Accumulation for GUI contents */
    val stagingByteBuffer = MemoryUtil.memAlloc(guiBufferSize)
    val stagingDraws = mutableListOf<Pair<Int, Int>>()

    var previousTexture = -1
    var sameTextureCount = 0
    var previousOffset = 0

    val drawer : InternalGuiDrawer = object : InternalGuiDrawer(gui) {

        val sx : Float
                get() = 1.0F / gui.viewportWidth.toFloat()
        val sy : Float
                get() = 1.0F / gui.viewportHeight.toFloat()

        fun vertex(a: Int, b: Int) {
            stagingByteBuffer.putFloat(-1.0F + 2.0F * (a * sx))
            stagingByteBuffer.putFloat(1.0F - 2.0F * (b * sy))
        }

        fun vertex(a: Float, b: Float) {
            stagingByteBuffer.putFloat(-1.0F + 2.0F * (a * sx))
            stagingByteBuffer.putFloat(1.0F - 2.0F * (b * sy))
        }

        fun texCoord(a: Float, b: Float) {
            stagingByteBuffer.putFloat(a)
            stagingByteBuffer.putFloat(b)
        }

        fun color(color: Vector4fc?) {
            stagingByteBuffer.putFloat(color?.x() ?: 1.0F)
            stagingByteBuffer.putFloat(color?.y() ?: 1.0F)
            stagingByteBuffer.putFloat(color?.z() ?: 1.0F)
            stagingByteBuffer.putFloat(color?.w() ?: 1.0F)
        }

        fun textureId(id: Int) {
            stagingByteBuffer.putInt(id)
        }

        override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: String?, color: Vector4fc?) {
            val vulkanTexture = (if (texture != null) backend.textures.getOrLoadTexture2D(texture) else backend.textures.defaultTexture2D)
                            as VulkanTexture2D

            val translatedId = virtualTexturingContext.translate(vulkanTexture)

            if(translatedId != -1 && previousTexture != translatedId)
                writeOut()

            previousTexture = translatedId

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)
            textureId(translatedId)

            vertex((startX), (startY + height))
            texCoord(textureStartX, textureEndY)
            color(color)
            textureId(translatedId)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)
            textureId(translatedId)

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)
            textureId(translatedId)

            vertex((startX + width), (startY))
            texCoord(textureEndX, textureStartY)
            color(color)
            textureId(translatedId)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)
            textureId(translatedId)

            sameTextureCount++
        }

        override fun drawQuad(startX: Float, startY: Float, width: Float, height: Float, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: VulkanTexture2D, color: Vector4fc?) {
            val translatedId = virtualTexturingContext.translate(texture)

            if(translatedId != -1 && previousTexture != translatedId)
                writeOut()

            previousTexture = translatedId

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)
            textureId(translatedId)

            vertex((startX), (startY + height))
            texCoord(textureStartX, textureEndY)
            color(color)
            textureId(translatedId)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)
            textureId(translatedId)

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)
            textureId(translatedId)

            vertex((startX + width), (startY))
            texCoord(textureEndX, textureStartY)
            color(color)
            textureId(translatedId)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)
            textureId(translatedId)

            sameTextureCount++
        }

        override fun drawBoxWithCorners(posx: Int, posy: Int, width: Int, height: Int, cornerSizeDivider: Int, texture: String) {
            //val texture = "textures/gui/alignmentCheck.png"

            val cornerSizeDivider = 4

            val tileSize = 32
            val cornerSize = tileSize / cornerSizeDivider
            val tileInternalSize = tileSize - cornerSize * 2

            val insideWidth = width - cornerSize * 2
            val insideHeight = height - cornerSize * 2

            val firstCornerUV = 1.0F / cornerSizeDivider
            val lastCornerUV = 1.0F - firstCornerUV

            var acc : Int

            // Bottom-right
            drawBox(posx, posy, cornerSize, cornerSize, 0f, 1f, firstCornerUV, lastCornerUV, texture, null)
            drawBox(posx, posy + height - cornerSize, cornerSize, cornerSize, 0f, firstCornerUV, firstCornerUV, 0f, texture, null)

            drawBox(posx + width - cornerSize, posy, cornerSize, cornerSize, lastCornerUV, 1f, 1f, lastCornerUV, texture, null)
            drawBox(posx + width - cornerSize, posy + height - cornerSize, cornerSize, cornerSize, lastCornerUV, firstCornerUV, 1f, 0f, texture, null)

            // vertical sides
            acc = insideHeight
            while(acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                drawBox(posx, posy + cornerSize + insideHeight - acc, cornerSize, wat, 0f, cutUV, firstCornerUV, firstCornerUV, texture, null)
                acc -= wat
            }
            //drawBox(posx, posy + cornerSize, cornerSize, insideHeight, 0f, lastCornerUV, firstCornerUV, firstCornerUV, texture, null)
            acc = insideHeight
            while(acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                //drawBox(posx, posy + cornerSize + insideHeight - acc, cornerSize, wat, 0f, cutUV, firstCornerUV, firstCornerUV, texture, null)
                drawBox(posx + width - cornerSize, posy + cornerSize + insideHeight - acc, cornerSize, wat, lastCornerUV, cutUV, 1f, firstCornerUV, texture, null)
                acc -= wat
            }
            //drawBox(posx + width - cornerSize, posy + cornerSize, cornerSize, insideHeight, lastCornerUV, lastCornerUV, 1f, firstCornerUV, texture, null)

            // horizontal sides
            acc = insideWidth
            while(acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                drawBox(posx + cornerSize + insideWidth - acc, posy + height - cornerSize, wat, cornerSize, firstCornerUV, firstCornerUV, cutUV, 0f, texture, null)
                acc -= wat
            }
            //drawBox(posx + cornerSize, posy + height - cornerSize, insideWidth, cornerSize, firstCornerUV, firstCornerUV, lastCornerUV, 0f, texture, null)
            acc = insideWidth
            while(acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                drawBox(posx + cornerSize + insideWidth - acc, posy, wat, cornerSize, firstCornerUV, 1f, cutUV, lastCornerUV, texture, null)
                acc -= wat
            }
            //drawBox(posx + cornerSize, posy, insideWidth, cornerSize, firstCornerUV, 1f, lastCornerUV, lastCornerUV, texture, null)

            // inside box
            acc = insideWidth
            while(acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                //drawBox(posx + cornerSize + insideWidth - acc, posy, wat, cornerSize, firstCornerUV, 1f, cutUV, lastCornerUV, texture, null)

                var acc2 = insideHeight
                while(acc2 > 0) {
                    val wat2 = Math.min(acc2, tileInternalSize)
                    val cutUV2 = firstCornerUV + (lastCornerUV - firstCornerUV) * wat2.toFloat() / tileInternalSize
                    drawBox(posx + cornerSize + insideWidth - acc, posy + cornerSize + insideHeight - acc2, wat, wat2, firstCornerUV, cutUV2, cutUV, firstCornerUV, texture, null)
                    acc2 -= wat2
                }

                acc -= wat
            }
            //drawBox(posx + cornerSize, posy + cornerSize, insideWidth, insideHeight, firstCornerUV, lastCornerUV, lastCornerUV, firstCornerUV, texture, null)
        }

        override fun drawString(font: Font, xPosition: Int, yPosition: Int, text: String, cutoffLength: Int, color: Vector4fc) {
            fontRenderer.drawString(this, font, xPosition.toFloat(), yPosition.toFloat(), text, cutoffLength.toFloat(), color)
        }
    }

    fun writeOut() {
        val primitivesCount = 3 * 2 * sameTextureCount

        if(sameTextureCount > 0)
            stagingDraws.add(Pair(primitivesCount, previousOffset))
            //vkCmdDraw(commandBuffer, primitivesCount, 1, previousOffset, 0)

        //println("$sameTextureCount : $previousOffset")

        previousOffset += primitivesCount
        sameTextureCount = 0
    }

    override fun registerDrawingCommands(frame: Frame, commandBuffer: VkCommandBuffer) {
        stackPush().use {
            // Write the commands in the command buffer
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            //vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.layout, 1, descriptorPool.setsForFrame(frame), null as? IntArray)
            vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffers[frame].handle), MemoryStack.stackLongs(0))

            stagingByteBuffer.clear()
            previousOffset = 0
            sameTextureCount = 0

            this.commandBuffer = commandBuffer
            virtualTexturingContext = virtualTexturing.begin(commandBuffer, pipeline, sampler) {
                writeOut()

                for((primitivesCount, offset) in stagingDraws)
                    vkCmdDraw(commandBuffer, primitivesCount, 1, offset, 0)

                stagingDraws.clear()
            }
            gui.topLayer?.render(drawer)
            virtualTexturingContext.finish()

            // Upload the vertex buffer contents
            vertexBuffers[frame].apply {
                stagingByteBuffer.flip()
                this.upload(stagingByteBuffer)
            }

            //val testOffset = UniformTestOffset()
            //testOffset.offset.x = (Math.random().toFloat() - 0.5F) * 0.2F
            //descriptorPool.configure(frame, testOffset)
            //descriptorPool.configureTextureAndSampler(frame, "diffuseTexture", backend.textures.defaultTexture2D as VulkanTexture2D, sampler)
        }
    }

    override fun cleanup() {
        fontRenderer.cleanup()

        sampler.cleanup()
        vertexBuffers.cleanup()

        pipeline.cleanup()
        virtualTexturing.cleanup()

        descriptorPool.cleanup()

        MemoryUtil.memFree(stagingByteBuffer)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.vulkan.triangleTest")
    }

}