//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.ingame

import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.net.packets.PacketText
import xyz.chunkstories.api.world.WorldClientNetworkedRemote
import xyz.chunkstories.api.world.WorldMaster
import org.joml.Vector4f
import xyz.chunkstories.api.math.intToHex
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.spawnPlayer

/**
 * Childishly taunts you when you die and offers you the option to ragequit the game
 */
class DeathScreenUI(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val respawnButton = Button(this, 0, 0, 160, "#{ingame.respawn}")
    private val exitButton = Button(this, 0, 0, 160, "#{ingame.exit}")

    // Doesn't make sense to display this screen if not ingame
    private val ingameClient: IngameClient = gui.client.ingame!!

    init {
        this.respawnButton.action = Runnable {
            if (ingameClient.world is WorldMaster)
                (ingameClient.world as WorldImplementation).spawnPlayer(ingameClient.player)
            else
                (ingameClient.world as WorldClientNetworkedRemote).remoteServer.pushPacket(PacketText("world/respawn"))

            gui.popTopLayer()
        }

        this.exitButton.action = Runnable { gui.client.ingame!!.exitToMainMenu() }

        elements.add(respawnButton)
        elements.add(exitButton)
    }

    override fun render(drawer: GuiDrawer) {
        parentLayer?.render(drawer)

        drawer.drawBox(0, 0, gui.viewportWidth, gui.viewportHeight, Vector4f(0.0f, 0.0f, 0.0f, 0.5f))

        var color = "#"
        color += intToHex((Math.random() * 255).toInt())
        color += intToHex((Math.random() * 255).toInt())
        color += intToHex((Math.random() * 255).toInt())

        val titleFont = drawer.fonts.getFont("LiberationSans-Regular", 32f)
        val font = drawer.fonts.getFont("LiberationSans-Regular", 11f)

        drawer.drawStringWithShadow(titleFont, gui.viewportWidth / 2 - titleFont.getWidth("YOU DIEDED") / 2, gui.viewportHeight / 2 + 48, "#FF0000YOU DIEDED", -1, Vector4f(1f))
        drawer.drawStringWithShadow(font, gui.viewportWidth / 2 - font.getWidth("git --gud scrub") / 2, gui.viewportHeight / 2, color + "git --gud scrub", -1, Vector4f(1f))

        respawnButton.setPosition(gui.viewportWidth / 2 - respawnButton.width / 2, gui.viewportHeight / 2 - 64)
        exitButton.setPosition(gui.viewportWidth / 2 - exitButton.width / 2, gui.viewportHeight / 2 - 64 - 24)

        respawnButton.render(drawer)
        exitButton.render(drawer)

        // When the new entity arrives, pop this
        if (ingameClient.player.controlledEntity != null)
            gui.popTopLayer()

        // Make sure to ungrab the mouse
        val mouse = gui.mouse
        if (mouse.isGrabbed)
            mouse.isGrabbed = false
    }
}
