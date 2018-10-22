package io.xol.chunkstories.graphics.common

import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.api.gui.Fonts
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.GuiDrawer
import org.joml.Vector4f
import org.joml.Vector4fc

open class DummyGuiDrawer(override val gui: Gui) : GuiDrawer {
    override val fonts: Fonts
        get() = gui.fonts

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: String?, color: Vector4fc?) {

    }

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, texture: String?) =
            drawBox(startX, startY, width, height, 0F, 0F, 1F, 1F, texture, null)

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, texture: String?, color: Vector4fc?) =
            drawBox(startX, startY, width, height, 0F, 0F, 1F, 1F, texture, color)

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, color: Vector4fc?) =
            drawBox(startX, startY, width, height, 0F, 0F, 1F, 1F, null, color)

    override fun drawBoxWithCorners(posx: Int, posy: Int, width: Int, height: Int, cornerSizeDivider: Int, texture: String) {

    }

    override fun drawString(font: Font, xPosition: Int, yPosition: Int, text: String, cutoffLength: Int, color: Vector4fc) {

    }

    override fun drawString(font: Font, xPosition: Int, yPosition: Int, text: String, color: Vector4fc) =
            drawString(font, xPosition, yPosition, text, -1, color)

    override fun drawString(xPosition: Int, yPosition: Int, text: String) =
            drawString(fonts.defaultFont(), xPosition, yPosition, text, -1, white)

    override fun drawString(xPosition: Int, yPosition: Int, text: String, color: Vector4fc) =
            drawString(fonts.defaultFont(), xPosition, yPosition, text, -1, color)

    override fun drawStringWithShadow(font: Font, xPosition: Int, yPosition: Int, text: String, cutoffLength: Int, color: Vector4fc) {
        val colorDarkened = Vector4f(color).mul(0.5F)
        drawString(font, xPosition + 2, yPosition + 2, text, cutoffLength, colorDarkened)
        drawString(font, xPosition, yPosition, text, cutoffLength, color)
    }

    companion object {
        val white = Vector4f(1.0F)
    }
}