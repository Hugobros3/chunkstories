package io.xol.chunkstories.gui

import io.xol.chunkstories.api.gui.Fonts
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.Layer
import io.xol.chunkstories.api.input.Mouse
import io.xol.chunkstories.api.item.inventory.Inventory
import io.xol.chunkstories.client.ClientImplementation
import io.xol.chunkstories.gui.layer.ingame.InventoryView
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("client.gui")

class ClientGui(override val client: ClientImplementation) : Gui {

    override val fonts: Fonts by lazy { FontsLibrary(client.content) }
    override val mouse: Mouse
        get() = client.inputsManager.getMouse()
    override var topLayer: Layer? = null

    //TODO make this configurable
    open val guiScale = 2
    override val viewportWidth: Int
        get() = client.gameWindow.width / guiScale
    override val viewportHeight: Int
        get() = client.gameWindow.height / guiScale

    override fun hasFocus() = client.gameWindow.hasFocus()

    override fun localization() = client.content.localization()

    override fun openInventories(vararg inventories: Inventory) {
        val ingameClient = client.ingame ?: return logger.warn("Asked to open an inventory but the client currently isn't ingame !")

        ingameClient.ingameGuiLayer.focus(false)
        ingameClient.gui.topLayer = InventoryView(ingameClient.gui, ingameClient.ingameGuiLayer, inventories)
    }
}