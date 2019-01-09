//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.config

import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.util.configuration.Configuration
import org.joml.Vector4f

import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button

/** Asks the user if he wishes to have his logs uploaded to the game servers for debugging purposes  */
//TODO anonymize those (strip C:\Users\... and such)
class LogPolicyAsk(gui: Gui, parent: Layer) : Layer(gui, parent) {

    private val option = gui.client.configuration.get<Configuration.OptionString>(logPolicyConfigNode)

    private val acceptButton = Button(this, 0, 0, 150, "#{logpolicy.accept}")
    private val refuseButton = Button(this, 0, 0, 150, "#{logpolicy.deny}")

    private val logPolicyExplanationText = gui.localization().getLocalizedString("logpolicy.asktext")

    init {

        this.acceptButton.action = Runnable {
            option!!.trySetting("send")
            gui.client.configuration.save()
            this@LogPolicyAsk.gui.topLayer = parentLayer
        }

        this.refuseButton.action = Runnable {
            option!!.trySetting("dont")
            gui.client.configuration.save()
            this@LogPolicyAsk.gui.topLayer = parentLayer
        }

        elements.add(acceptButton)
        elements.add(refuseButton)
    }

    override fun render(drawer: GuiDrawer) {
        parentLayer?.render(drawer)

        drawer.drawBox(0, 0, gui.viewportWidth, gui.viewportHeight, 0f, 0f, 0f, 0f, null, Vector4f(0.0f, 0.0f, 0.0f, 0.5f))

        drawer.drawStringWithShadow(
                drawer.fonts.getFont("LiberationSans-Regular__aa", (16 * 1).toFloat()), 30,
                gui.viewportHeight - 64,
                gui.client.content.localization().getLocalizedString("logpolicy.title"), -1,
                Vector4f(1.0f))

        val logPolicyTextFont = drawer.fonts.getFont("LiberationSans-Regular__aa", 12f)

        drawer.drawString(logPolicyTextFont, 30, gui.viewportHeight - 128, logPolicyExplanationText, width - 60, Vector4f(1.0f))

        val buttonsSpacing = 4
        val buttonsPlusSpacingLength = acceptButton.width + refuseButton.width + buttonsSpacing

        acceptButton.setPosition(gui.viewportWidth / 2 - buttonsPlusSpacingLength / 2,
                gui.viewportHeight / 4 - 32)
        acceptButton.render(drawer)

        refuseButton.setPosition(
                gui.viewportWidth / 2 - buttonsPlusSpacingLength / 2 + buttonsSpacing + acceptButton.width,
                gui.viewportHeight / 4 - 32)
        refuseButton.render(drawer)
    }

    companion object {
        val logPolicyConfigNode = "client.game.logPolicy"
    }
}
