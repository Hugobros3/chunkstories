//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.api.gui.Layer
import io.xol.chunkstories.api.gui.elements.Button
import io.xol.chunkstories.api.input.Input
import io.xol.chunkstories.gui.layer.config.ModsSelection
import io.xol.chunkstories.gui.layer.config.OptionsScreen
import org.joml.Vector4f

/** The GUI code for the basic pause menu you bring about by pressing ESC  */
class PauseMenu internal constructor(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val resumeButton = Button(this, 0, 0, 160, "#{menu.resume}")
    private val optionsButton = Button(this, 0, 0, 160, "#{menu.options}")
    private val modsButton = Button(this, -100, 0, 160, "#{menu.mods}")
    private val exitButton = Button(this, 0, 0, 160, "#{menu.backto}")

    init {

        this.resumeButton.action = Runnable { gui.popTopLayer() }
        this.optionsButton.action = Runnable { gui.topLayer = OptionsScreen(gui, this@PauseMenu) }
        this.modsButton.action = Runnable { gui.topLayer = ModsSelection(gui, this@PauseMenu) }
        this.exitButton.action = Runnable { gui.client.ingame!!.exitToMainMenu() }

        elements.add(resumeButton)
        elements.add(optionsButton)
        // elements.add(modsButton);
        elements.add(exitButton)
    }

    override fun render(drawer: GuiDrawer?) {
        parentLayer?.render(drawer)

        val font = drawer!!.fonts.getFont("LiberationSans-Regular", 11f)
        val pauseText = gui.localization().getLocalizedString("ingame.pause")
        drawer.drawStringWithShadow(font,
                gui.viewportWidth / 2 - font.getWidth(pauseText) / 2,
                gui.viewportHeight / 2 + 48 * 3, pauseText, -1, Vector4f(1f))

        resumeButton.setPosition(gui.viewportWidth / 2 - resumeButton.width / 2, gui.viewportHeight / 2 + 24 * 2)
        optionsButton.setPosition(resumeButton.positionX, gui.viewportHeight / 2 + 24)
        exitButton.setPosition(resumeButton.positionX, gui.viewportHeight / 2 - 24)

        resumeButton.render(drawer)
        optionsButton.render(drawer)
        exitButton.render(drawer)
    }

    override fun handleInput(input: Input): Boolean {
        if (input.name == "exit") {
            gui.popTopLayer()
            return true
        }

        super.handleInput(input)
        return true
    }
}
