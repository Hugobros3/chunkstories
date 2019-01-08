//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config

import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.GuiDrawer
import org.joml.Vector4f

import io.xol.chunkstories.api.gui.Layer
import io.xol.chunkstories.gui.layer.config.OptionsScreen.ConfigButtonKey

internal class KeyBindSelectionOverlay(gui: Gui, options: Layer, private val callback: ConfigButtonKey) : Layer(gui, options) {
    override fun render(drawer: GuiDrawer) {
        this.parentLayer?.render(drawer)

        drawer.drawBox(0, 0, gui.viewportWidth, gui.viewportHeight, Vector4f(0.0f, 0.0f, 0.0f, 0.5f))

        //TODO localization
        val plz = "Please press a key"

        val font = drawer.fonts.getFont("LiberationSans-Regular", 11f)
        drawer.drawStringWithShadow(font, gui.viewportWidth / 2 - font.getWidth(plz) * 2, gui.viewportHeight / 2, plz, -1, Vector4f(1f))
    }

    fun setKeyTo(k: Int) {
        callback.callBack(k)
        gui.topLayer = parentLayer
    }
}
