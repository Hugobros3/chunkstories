//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.GuiDrawer
import org.joml.Vector4f

import io.xol.chunkstories.api.gui.Layer
import io.xol.chunkstories.api.gui.elements.Button
import io.xol.chunkstories.client.net.ClientConnectionSequence
import io.xol.chunkstories.gui.layer.MainMenu
import io.xol.chunkstories.gui.layer.MessageBox

/** GUI overlay that tells you about the progress of connecting to a server  */
class RemoteConnectionGuiLayer(scene: Gui, parent: Layer, private val connectionSequence: ClientConnectionSequence) : Layer(scene, parent) {
    internal var exitButton = Button(this, 0, 0, 160, "#{connection.cancel}")

    init {
        this.exitButton.setAction { gui.client.ingame?.exitToMainMenu() ?: let { gui.topLayer = MainMenu(gui, null) } }
        elements.add(exitButton)
    }

    override fun render(drawer: GuiDrawer) {
        parentLayer?.render(drawer)

        val color = "#606060"
        val font = drawer.fonts.getFont("LiberationSans-Regular", 11f)

        val connection = "Connecting, please wait"

        drawer.drawStringWithShadow(font,
                gui.viewportWidth / 2 - font.getWidth(connection) * 2,
                gui.viewportHeight / 2 + 48 * 3, connection, -1, Vector4f(1f))

        val currentConnectionStep = connectionSequence.status.stepText

        drawer.drawStringWithShadow(font,
                gui.viewportWidth / 2 - font.getWidth(currentConnectionStep) * 2,
                gui.viewportHeight / 2 + 32 * 3, color + currentConnectionStep, -1,
                Vector4f(1f))

        exitButton.setPosition(gui.viewportWidth / 2 - exitButton.getWidth() / 2,
                gui.viewportHeight / 2 - 24)

        exitButton.render(drawer)

        // Once the connection sequence is done, we hide this overlay
        if (connectionSequence.isDone)
            this.gui.topLayer = parentLayer

        val connectionFailureReason = connectionSequence.wasAborted()
        if (connectionFailureReason != null)
            gui.client.ingame?.exitToMainMenu(connectionFailureReason) ?: let { gui.topLayer = MessageBox(gui, MainMenu(gui, null), connectionFailureReason) }
    }
}
