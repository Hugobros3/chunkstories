package xyz.chunkstories.graphics.vulkan.systems.gui

import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.graphics.common.DummyGuiDrawer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.common.Primitive
import xyz.chunkstories.graphics.vulkan.Pipeline
import xyz.chunkstories.graphics.vulkan.VulkanGraphicsBackend
import xyz.chunkstories.graphics.vulkan.buffers.VulkanVertexBuffer
import xyz.chunkstories.graphics.vulkan.graph.VulkanPass
import xyz.chunkstories.graphics.vulkan.resources.InflightFrameResource
import xyz.chunkstories.graphics.vulkan.swapchain.VulkanFrame
import xyz.chunkstories.graphics.vulkan.systems.VulkanDrawingSystem
//import xyz.chunkstories.graphics.vulkan.textures.VirtualTexturing
import xyz.chunkstories.graphics.vulkan.textures.VulkanSampler
import xyz.chunkstories.graphics.vulkan.textures.VulkanTexture2D
import xyz.chunkstories.graphics.vulkan.vertexInputConfiguration
import xyz.chunkstories.gui.ClientGui
import org.joml.Vector4fc
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.graphics.common.gui.InternalGuiDrawer
import xyz.chunkstories.graphics.vulkan.memory.MemoryUsagePattern
import xyz.chunkstories.graphics.vulkan.resources.DescriptorSetsMegapool

internal const val guiBufferSize = 2 * 1024 * 1024

class VulkanGuiDrawer(pass: VulkanPass, val gui: ClientGui) : VulkanDrawingSystem(pass) {

    val backend: VulkanGraphicsBackend
        get() = pass.backend

    val fontRenderer = VulkanFontRenderer(backend)
    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding(0)
            stride(2 * 4 + 2 * 4 + 4 * 4)
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

        /*attribute {
            binding(0)
            location(program.vertexInputs.find { it.name == "textureIdIn" }?.location!!)
            format(VK_FORMAT_R32_SINT)
            offset(2 * 4 + 2 * 4 + 4 * 4)
        }*/
    }

    val program = backend.shaderFactory.createProgram("gui")
    val pipeline = Pipeline(backend, program, pass, vertexInputConfiguration, Primitive.TRIANGLES, FaceCullingMode.CULL_BACK)
    val sampler = VulkanSampler(backend)
    val vertexBuffers: InflightFrameResource<VulkanVertexBuffer>

    init {
        vertexBuffers = InflightFrameResource(backend) {
            VulkanVertexBuffer(backend, guiBufferSize.toLong(), MemoryUsagePattern.SEMI_STATIC)
        }
    }

    //TODO this is hacky af, fix this plz
    lateinit var commandBuffer: VkCommandBuffer

    /** Accumulation for GUI contents */
    val stagingByteBuffer = MemoryUtil.memAlloc(guiBufferSize)
    var recyclingBind = mutableListOf<DescriptorSetsMegapool.ShaderBindingContext>()

    var previousTexture: Texture2D? = null
    var sameTextureCount = 0
    var previousOffset = 0

    val drawer: InternalGuiDrawer = object : InternalGuiDrawer(gui) {

        val sx: Float
            get() = 1.0F / gui.viewportWidth.toFloat()
        val sy: Float
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

        override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: String?, color: Vector4fc?) {
            val vulkanTexture = if (texture != null) backend.textures.getOrLoadTexture2D(texture) else backend.textures.getOrLoadTexture2D("textures/white.png")

            if (previousTexture != vulkanTexture) {
                afterTextureSwitch()

                val bindingCtx = backend.descriptorMegapool.getBindingContext(pipeline)
                bindingCtx.bindTextureAndSampler("currentTexture", vulkanTexture, sampler)
                bindingCtx.preDraw(commandBuffer)
                recyclingBind.add(bindingCtx)
            }

            previousTexture = vulkanTexture

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)

            vertex((startX), (startY + height))
            texCoord(textureStartX, textureEndY)
            color(color)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)

            vertex((startX + width), (startY))
            texCoord(textureEndX, textureStartY)
            color(color)

            sameTextureCount++
        }

        override fun drawQuad(startX: Float, startY: Float, width: Float, height: Float, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: Texture2D, color: Vector4fc?) {
            val translatedId = 0

            val vulkanTexture = texture as VulkanTexture2D

            if (previousTexture != vulkanTexture) {
                afterTextureSwitch()

                val bindingCtx = backend.descriptorMegapool.getBindingContext(pipeline)
                bindingCtx.bindTextureAndSampler("currentTexture", vulkanTexture, sampler)
                bindingCtx.preDraw(commandBuffer)
                recyclingBind.add(bindingCtx)
            }

            previousTexture = vulkanTexture

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)

            vertex((startX), (startY + height))
            texCoord(textureStartX, textureEndY)
            color(color)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)

            vertex((startX), startY)
            texCoord(textureStartX, textureStartY)
            color(color)

            vertex((startX + width), (startY + height))
            texCoord(textureEndX, textureEndY)
            color(color)

            vertex((startX + width), (startY))
            texCoord(textureEndX, textureStartY)
            color(color)

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

            var acc: Int

            // Bottom-right
            drawBox(posx, posy, cornerSize, cornerSize, 0f, 1f, firstCornerUV, lastCornerUV, texture, null)
            drawBox(posx, posy + height - cornerSize, cornerSize, cornerSize, 0f, firstCornerUV, firstCornerUV, 0f, texture, null)

            drawBox(posx + width - cornerSize, posy, cornerSize, cornerSize, lastCornerUV, 1f, 1f, lastCornerUV, texture, null)
            drawBox(posx + width - cornerSize, posy + height - cornerSize, cornerSize, cornerSize, lastCornerUV, firstCornerUV, 1f, 0f, texture, null)

            // vertical sides
            acc = insideHeight
            while (acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                drawBox(posx, posy + cornerSize + insideHeight - acc, cornerSize, wat, 0f, cutUV, firstCornerUV, firstCornerUV, texture, null)
                acc -= wat
            }
            //drawBox(posx, posy + cornerSize, cornerSize, insideHeight, 0f, lastCornerUV, firstCornerUV, firstCornerUV, texture, null)
            acc = insideHeight
            while (acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                //drawBox(posx, posy + cornerSize + insideHeight - acc, cornerSize, wat, 0f, cutUV, firstCornerUV, firstCornerUV, texture, null)
                drawBox(posx + width - cornerSize, posy + cornerSize + insideHeight - acc, cornerSize, wat, lastCornerUV, cutUV, 1f, firstCornerUV, texture, null)
                acc -= wat
            }
            //drawBox(posx + width - cornerSize, posy + cornerSize, cornerSize, insideHeight, lastCornerUV, lastCornerUV, 1f, firstCornerUV, texture, null)

            // horizontal sides
            acc = insideWidth
            while (acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                drawBox(posx + cornerSize + insideWidth - acc, posy + height - cornerSize, wat, cornerSize, firstCornerUV, firstCornerUV, cutUV, 0f, texture, null)
                acc -= wat
            }
            //drawBox(posx + cornerSize, posy + height - cornerSize, insideWidth, cornerSize, firstCornerUV, firstCornerUV, lastCornerUV, 0f, texture, null)
            acc = insideWidth
            while (acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                drawBox(posx + cornerSize + insideWidth - acc, posy, wat, cornerSize, firstCornerUV, 1f, cutUV, lastCornerUV, texture, null)
                acc -= wat
            }
            //drawBox(posx + cornerSize, posy, insideWidth, cornerSize, firstCornerUV, 1f, lastCornerUV, lastCornerUV, texture, null)

            // inside box
            acc = insideWidth
            while (acc > 0) {
                val wat = Math.min(acc, tileInternalSize)
                val cutUV = firstCornerUV + (lastCornerUV - firstCornerUV) * wat.toFloat() / tileInternalSize
                //drawBox(posx + cornerSize + insideWidth - acc, posy, wat, cornerSize, firstCornerUV, 1f, cutUV, lastCornerUV, texture, null)

                var acc2 = insideHeight
                while (acc2 > 0) {
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

    fun afterTextureSwitch() {
        val primitivesCount = 3 * 2 * sameTextureCount

        if (sameTextureCount > 0)
            vkCmdDraw(this.commandBuffer, primitivesCount, 1, previousOffset, 0)

        previousOffset += primitivesCount
        sameTextureCount = 0
    }

    override fun registerDrawingCommands(frame: VulkanFrame, ctx: SystemExecutionContext, commandBuffer: VkCommandBuffer) {
        stackPush().use {
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
            vkCmdBindVertexBuffers(commandBuffer, 0, MemoryStack.stackLongs(vertexBuffers[frame].handle), MemoryStack.stackLongs(0))

            val bindingCtx = backend.descriptorMegapool.getBindingContext(pipeline)
            bindingCtx.bindTextureAndSampler("currentTexture", backend.textures.getOrLoadTexture2D("textures/white.png"), sampler)
            bindingCtx.preDraw(commandBuffer)
            recyclingBind.add(bindingCtx)

            stagingByteBuffer.clear()
            previousOffset = 0
            sameTextureCount = 0
            previousTexture = null

            this.commandBuffer = commandBuffer

            gui.topLayer?.render(drawer)
            afterTextureSwitch()

            // Upload the vertex buffer contents
            vertexBuffers[frame].apply {
                stagingByteBuffer.flip()
                this.upload(stagingByteBuffer)
            }

            val bindingCtxes = recyclingBind.toList()
            recyclingBind.clear()
            frame.recyclingTasks.add {
                bindingCtxes.forEach { it.recycle() }
            }
        }
    }

    override fun cleanup() {
        fontRenderer.cleanup()

        sampler.cleanup()
        vertexBuffers.cleanup()

        pipeline.cleanup()
        program.cleanup()
        //virtualTexturing.cleanup()

        MemoryUtil.memFree(stagingByteBuffer)
    }

    companion object {
        val logger = LoggerFactory.getLogger("client.gfx_vk.gui")
    }

}