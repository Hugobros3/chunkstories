//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.client.IngameClient
import io.xol.chunkstories.api.gui.Font
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.api.gui.Layer
import io.xol.chunkstories.api.gui.elements.Button
import io.xol.chunkstories.api.input.Mouse
import io.xol.chunkstories.api.math.HexTools
import io.xol.chunkstories.api.net.packets.PacketText
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote
import io.xol.chunkstories.api.world.WorldMaster
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.gui.layer.MainMenu
import org.joml.Vector4f

/**
 * Childishly taunts you when you die and offers you the option to ragequit the game
 */
class DeathScreen(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val respawnButton = Button(this, 0, 0, 160, "#{ingame.respawn}")
    private val exitButton = Button(this, 0, 0, 160, "#{ingame.exit}")

    // Doesn't make sense to display this screen if not ingame
    private val ingameClient: IngameClient = gui.client.ingame!!

    init {
        this.respawnButton.action = Runnable {
            if (ingameClient?.world is WorldMaster)
                (ingameClient.world as WorldMaster).spawnPlayer(ingameClient.player)
            else
                (ingameClient?.world as WorldClientNetworkedRemote).remoteServer.pushPacket(PacketText("world/respawn"))

            gui.popTopLayer()
        }

        this.exitButton.action = Runnable { gui.topLayer = MainMenu(gui, null) }

        elements.add(respawnButton)
        elements.add(exitButton)
    }

    override fun render(drawer: GuiDrawer) {
        parentLayer?.render(drawer)

        drawer.drawBox(0, 0, gui.viewportWidth, gui.viewportHeight, Vector4f(0.0f, 0.0f, 0.0f, 0.5f))

        var color = "#"
        color += HexTools.intToHex((Math.random() * 255).toInt())
        color += HexTools.intToHex((Math.random() * 255).toInt())
        color += HexTools.intToHex((Math.random() * 255).toInt())

        val font = drawer.fonts.getFont("LiberationSans-Regular", 11f)

        drawer.drawStringWithShadow(font, gui.viewportWidth / 2 - font.getWidth("YOU DIEDED") / 2, gui.viewportHeight / 2 + 48 * 3, "#FF0000YOU DIEDED", -1, Vector4f(1f))
        drawer.drawStringWithShadow(font, gui.viewportWidth / 2 - font.getWidth("git --gud scrub") / 2, gui.viewportHeight / 2 + 36 * 3, color + "git --gud scrub", -1, Vector4f(1f))

        respawnButton.setPosition(gui.viewportWidth / 2 - respawnButton.width / 2, gui.viewportHeight / 2 + 48)
        exitButton.setPosition(gui.viewportWidth / 2 - exitButton.width / 2, gui.viewportHeight / 2 - 24)

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
