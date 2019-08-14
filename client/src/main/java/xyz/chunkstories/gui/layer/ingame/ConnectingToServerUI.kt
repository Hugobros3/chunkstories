//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.ingame

import org.joml.Vector4f
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.client.net.ClientConnectionSequence
import xyz.chunkstories.gui.layer.MainMenuUI
import xyz.chunkstories.gui.layer.MessageBoxUI

/** GUI overlay that tells you about the progress of connecting to a server  */
class ConnectingToServerUI(gui: Gui, parent: Layer?, private val connectionSequence: ClientConnectionSequence) : Layer(gui, parent) {
    internal var exitButton = Button(this, 0, 0, 160, "#{connection.cancel}")

    init {
        this.exitButton.action = Runnable {
            connectionSequence.abort()
            this.gui.client.ingame?.exitToMainMenu() ?: let { this.gui.topLayer = MainMenuUI(this.gui, null) }
        }
        elements.add(exitButton)
    }

    override fun render(drawer: GuiDrawer) {
        parentLayer?.render(drawer)

        val color = "#606060"
        val font = drawer.fonts.getFont("LiberationSans-Regular", 16f)

        val connection = "Connecting, please wait"

        drawer.drawStringWithShadow(font,
                gui.viewportWidth / 2 - font.getWidth(connection) / 2,
                gui.viewportHeight - 64, connection, -1, Vector4f(1f))

        val status = connectionSequence.state
        val statusText = status.text

        drawer.drawStringWithShadow(font,
                gui.viewportWidth / 2 - font.getWidth(statusText) / 2,
                gui.viewportHeight / 2 + 24, color + statusText, -1,
                Vector4f(1f))

        exitButton.setPosition(gui.viewportWidth / 2 - exitButton.width / 2, gui.viewportHeight / 3 - 24)
        exitButton.render(drawer)

        // Once the connection sequence is done, we hide this overlay
        if (connectionSequence.isDone)
            this.gui.topLayer = parentLayer

        when (status) {
            is ClientConnectionSequence.Finished -> gui.popTopLayer()
            is ClientConnectionSequence.Failure -> gui.client.ingame?.exitToMainMenu(statusText) ?: let { gui.topLayer = MessageBoxUI(gui, MainMenuUI(gui, null), "Connection failure", statusText) }
        }
    }
}
