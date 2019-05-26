package xyz.chunkstories.graphics.common

import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.gui.Fonts
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import org.joml.Vector4f
import org.joml.Vector4fc
import xyz.chunkstories.api.graphics.rendergraph.SystemExecutionContext

open class DummyGuiDrawer(override val gui: Gui) : GuiDrawer {
    override val fonts: Fonts
        get() = gui.fonts

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, textureStartX: Float, textureStartY: Float, textureEndX: Float, textureEndY: Float, texture: String?, color: Vector4fc?) {

    }

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, texture: String?) =
            drawBox(startX, startY, width, height, 0F, 1F, 1F, 0F, texture, null)

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, texture: String?, color: Vector4fc?) =
            drawBox(startX, startY, width, height, 0F, 1F, 1F, 0F, texture, color)

    override fun drawBox(startX: Int, startY: Int, width: Int, height: Int, color: Vector4fc?) =
            drawBox(startX, startY, width, height, 0F, 1F, 1F, 0F, null, color)

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
        val colorDarkened = Vector4f(color)
        colorDarkened.x *= 0.25f
        colorDarkened.y *= 0.25f
        colorDarkened.z *= 0.25f
        drawString(font, xPosition + 1, yPosition - 1, text, cutoffLength, colorDarkened)
        drawString(font, xPosition, yPosition, text, cutoffLength, color)
    }

    companion object {
        val white = Vector4f(1.0F)
    }

    override fun withScissor(startX: Int, startY: Int, width: Int, height: Int, code: () -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setup(dslCode: SystemExecutionContext.() -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}