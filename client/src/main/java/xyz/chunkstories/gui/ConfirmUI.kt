package xyz.chunkstories.gui

import org.joml.Vector4f
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button

class ConfirmUI (gui: Gui, parent: Layer?, private val title: String, private val message: String, private val callback: (Boolean) -> Unit) : Layer(gui, parent) {
    private val cancel = Button(this, 0, 0, 150, "#{menu.cancel}")
    private val okButton = Button(this, 0, 0, 150, "#{menu.ok}")

    init {
        cancel.action = Runnable {
            gui.popTopLayer()
            callback(false)
        }
        okButton.action = Runnable {
            gui.popTopLayer()
            callback(true)
        }
        elements.add(okButton)
    }

    override fun render(drawer: GuiDrawer) {
        parentLayer?.render(drawer)

        drawer.drawBox(0, 0, gui.viewportWidth, gui.viewportHeight, 0f, 0f, 0f, 0f, null, Vector4f(0.0f, 0.0f, 0.0f, 0.5f))

        val titleFont = drawer.fonts.getFont("LiberationSans-Regular", 16f)
        val centeringOffset = titleFont.getWidth(title)
        drawer.drawStringWithShadow(titleFont, gui.viewportWidth / 2 - centeringOffset / 2, gui.viewportHeight / 2 + 64, title, -1, Vector4f(1f, 0.2f, 0.2f, 1f))

        val font = drawer.fonts.getFont("LiberationSans-Regular", 14f)
        val centeringOffset2 = font.getWidth(message)
        drawer.drawStringWithShadow(font, gui.viewportWidth / 2 - centeringOffset2 / 2, gui.viewportHeight / 2, message, -1, Vector4f(1f, 0.2f, 0.2f, 1f))

        okButton.setPosition(gui.viewportWidth / 2 - okButton.width / 2, gui.viewportHeight / 2 - 64)
        okButton.render(drawer)
    }
}
