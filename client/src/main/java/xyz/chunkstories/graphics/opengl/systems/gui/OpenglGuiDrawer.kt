package xyz.chunkstories.graphics.opengl.systems.gui

import org.joml.Vector4fc
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.VertexFormat
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext
import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.graphics.common.DummyGuiDrawer
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.opengl.*
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.systems.OpenglDrawingSystem
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D

import org.lwjgl.opengl.GL30.*
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer

class OpenglGuiDrawer(pass: OpenglPass) : OpenglDrawingSystem(pass) {

    val backend: OpenglGraphicsBackend
        get() = pass.backend

    val gui: Gui
        get() = backend.window.client.gui

    val vertexInputConfiguration = vertexInputConfiguration {
        binding {
            binding = 0
            stride = 32
            inputRate = InputRate.PER_VERTEX
        }

        attribute {
            binding = 0
            locationName = "vertexIn"
            format = Pair(VertexFormat.FLOAT, 2)
            offset = 0
        }

        attribute {
            binding = 0
            locationName = "texCoordIn"
            format = Pair(VertexFormat.FLOAT, 2)
            offset = 8
        }

        attribute {
            binding = 0
            locationName = "colorIn"
            format = Pair(VertexFormat.FLOAT, 4)
            offset = 16
        }
    }

    val program = backend.shaderFactory.createProgram("gui")
    val pipeline = FakePSO(backend, program, pass, vertexInputConfiguration, FaceCullingMode.CULL_BACK)

    val vertexBuffer = OpenglVertexBuffer(backend)

    val guiBufferSize = 2 * 1024 * 1024
    val stagingByteBuffer = MemoryUtil.memAlloc(guiBufferSize)
    val drawCalls = mutableListOf<Triple<Int, Int, OpenglTexture2D?>>()

    var currentTexture: Texture2D? = null
    var sameTextureCount = 0
    var previousOffset = 0

    val drawer: DummyGuiDrawer = object : DummyGuiDrawer(gui) {

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
            //val vulkanTexture = if (texture != null) backend.textures.getOrLoadTexture2D(texture) else backend.textures.getOrLoadTexture2D("textures/white.png")
            val glTexture: OpenglTexture2D? = null

            if (currentTexture != glTexture) {
                atTextureSwap(glTexture)
            }

            currentTexture = glTexture

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

        /*override fun drawQuad(startX: Float, startY: Float, width: Float, height: Float, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: VulkanTexture2D, color: Vector4fc?) {
            val translatedId = 0

            if (currentTexture != texture) {
                afterTextureSwitch()

                val bindingCtx = backend.descriptorMegapool.getBindingContext(pipeline)
                bindingCtx.bindTextureAndSampler("currentTexture", texture, sampler)
                bindingCtx.preDraw(commandBuffer)
                recyclingBind.add(bindingCtx)
            }

            currentTexture = texture

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
        }*/

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
            //fontRenderer.drawString(this, font, xPosition.toFloat(), yPosition.toFloat(), text, cutoffLength.toFloat(), color)
        }
    }

    fun atTextureSwap(glTexture: OpenglTexture2D?) {
        val primitivesCount = 3 * 2 * sameTextureCount

        if (sameTextureCount > 0) {
            drawCalls += Triple(primitivesCount, previousOffset, glTexture)
            //vkCmdDraw(this.commandBuffer, primitivesCount, 1, previousOffset, 0)
        }

        previousOffset += primitivesCount
        sameTextureCount = 0
    }

    override fun executeDrawingCommands(frame: OpenglFrame, ctx: SystemExecutionContext) {
        pipeline.bind()

        drawCalls.clear()
        stagingByteBuffer.clear()
        previousOffset = 0
        sameTextureCount = 0
        currentTexture = null

        gui.topLayer?.render(drawer)
        atTextureSwap(null)

        stagingByteBuffer.flip()
        vertexBuffer.upload(stagingByteBuffer)

        pipeline.bindVertexBuffer(0, vertexBuffer)
        for(drawcall in drawCalls) {
            glDrawArrays(GL_TRIANGLES, drawcall.second, drawcall.first)
        }

        pipeline.unbind()
    }

    override fun cleanup() {
        memFree(stagingByteBuffer)

        vertexBuffer.cleanup()

        pipeline.cleanup()
        program.cleanup()
    }
}