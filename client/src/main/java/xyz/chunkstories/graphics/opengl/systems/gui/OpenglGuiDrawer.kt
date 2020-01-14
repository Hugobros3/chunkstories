package xyz.chunkstories.graphics.opengl.systems.gui

import org.joml.Vector4fc
import org.joml.Vector4i
import org.lwjgl.opengl.GL11
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import xyz.chunkstories.api.graphics.Texture2D
import xyz.chunkstories.api.graphics.VertexFormat
import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.graphics.common.FaceCullingMode
import xyz.chunkstories.graphics.opengl.*
import xyz.chunkstories.graphics.opengl.graph.OpenglPass
import xyz.chunkstories.graphics.opengl.systems.OpenglDrawingSystem
import xyz.chunkstories.graphics.opengl.textures.OpenglTexture2D

import org.lwjgl.opengl.GL30.*
import xyz.chunkstories.api.graphics.systems.drawing.DrawingSystem
import xyz.chunkstories.graphics.common.gui.InternalGuiDrawer
import xyz.chunkstories.graphics.opengl.buffers.OpenglVertexBuffer
import xyz.chunkstories.graphics.opengl.graph.OpenglPassInstance
import xyz.chunkstories.graphics.opengl.shaders.bindTexture
import xyz.chunkstories.gui.ClientGui

class OpenglGuiDrawer(pass: OpenglPass, dslCode: (DrawingSystem) -> Unit) : OpenglDrawingSystem(pass) {

    val backend: OpenglGraphicsBackend
        get() = pass.backend

    val gui: ClientGui
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

    init {
        dslCode(this)
    }

    val program = backend.shaderFactory.createProgram("gui")
    val pipeline = FakePSO(backend, program, pass, vertexInputConfiguration, FaceCullingMode.CULL_BACK)

    val fontRenderer = OpenglFontRenderer(backend)

    val vertexBuffer = OpenglVertexBuffer(backend)

    val guiBufferSize = 2 * 1024 * 1024
    val stagingByteBuffer = MemoryUtil.memAlloc(guiBufferSize)
    val drawCalls = mutableListOf<Drawcall>()

    data class Drawcall(val count: Int, val first: Int, val texture: OpenglTexture2D?, val scissor: Vector4i? = null)

    var currentTexture: Texture2D? = null
    var sameTextureCount = 0
    var previousOffset = 0

    var scissor: Vector4i? = null

    fun atTextureSwap() {
        val primitivesCount = 3 * 2 * sameTextureCount

        if (sameTextureCount > 0) {
            drawCalls += Drawcall(primitivesCount, previousOffset, currentTexture as OpenglTexture2D?, scissor)
        }

        previousOffset += primitivesCount
        sameTextureCount = 0
    }

    override fun executeDrawingCommands(context: OpenglPassInstance) {
        val drawer = object : InternalGuiDrawer(gui) {
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
                val glTexture = if (texture != null) backend.textures.getOrLoadTexture2D(texture) else backend.textures.getOrLoadTexture2D("textures/white.png")
                //val glTexture: OpenglTexture2D? = null

                if (currentTexture != glTexture) {
                    atTextureSwap()
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

            override fun drawQuad(startX: Float, startY: Float, width: Float, height: Float, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: Texture2D, color: Vector4fc?) {
                val translatedId = 0

                if (currentTexture != texture) {
                    atTextureSwap()
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

            override fun withScissor(startX: Int, startY: Int, width: Int, height: Int, code: () -> Unit) {
                val gui = this@OpenglGuiDrawer.gui as ClientGui
                val s = gui.guiScale

                GL11.glEnable(GL11.GL_SCISSOR_TEST)
                //GL11.glScissor(startX * s, startY * s, width, height * s)
                scissor = Vector4i(startX * s, context.renderTargetSize.y - height * s - startY * s, width * s, height * s)
                atTextureSwap()
                code()
                atTextureSwap()
                scissor = null
            }
        }

        pipeline.bind()

        drawCalls.clear()
        stagingByteBuffer.clear()
        previousOffset = 0
        sameTextureCount = 0
        currentTexture = null

        gui.updateGuiScale()
        gui.topLayer?.render(drawer)
        atTextureSwap()

        stagingByteBuffer.flip()
        vertexBuffer.upload(stagingByteBuffer)

        pipeline.bindVertexBuffer(0, vertexBuffer)

        var cscissor: Vector4i? = null
        for(drawcall in drawCalls) {
            val scissor = drawcall.scissor
            if(scissor != cscissor) {
                if(scissor != null) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST)
                    //GL11.glScissor(startX * s, startY * s, width, height * s)
                    GL11.glScissor(scissor.x, scissor.y, scissor.z, scissor.w)
                } else {
                    GL11.glDisable(GL11.GL_SCISSOR_TEST)
                    GL11.glScissor(0, 0, context.renderTargetSize.x, context.renderTargetSize.y)
                }
            }

            drawcall.texture?.let { pipeline.bindTexture("currentTexture", 0, it, null) }
            glDrawArrays(GL_TRIANGLES, drawcall.first, drawcall.count)

            cscissor = scissor
        }

        pipeline.unbind()
    }

    override fun cleanup() {
        memFree(stagingByteBuffer)

        vertexBuffer.cleanup()

        pipeline.cleanup()
        program.cleanup()

        fontRenderer.cleanup()
    }
}