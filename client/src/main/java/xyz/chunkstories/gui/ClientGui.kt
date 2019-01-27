package xyz.chunkstories.gui

import xyz.chunkstories.api.gui.Fonts
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.input.Mouse
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.gui.layer.ingame.InventoryView
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger("client.gui")

class ClientGui(override val client: ClientImplementation) : Gui {

    override val fonts: Fonts by lazy { FontsLibrary(client.content) }

    override var topLayer: Layer? = null

    //TODO make this configurable
    open val guiScale: Int
        get() {
            if(guiScaleOverride != -1)
                return guiScaleOverride

            val scaleHorizontal = Math.floor(client.gameWindow.width / 512.0).toInt()
            val scaleVertical = Math.floor(client.gameWindow.height / 320.0).toInt()

            val minScale = Math.min(scaleHorizontal, scaleVertical)
            if(minScale in 1..16)
                return minScale
            return 1
        }

    var guiScaleOverride = -1

    override val viewportWidth: Int
        get() = client.gameWindow.width / guiScale
    override val viewportHeight: Int
        get() = client.gameWindow.height / guiScale

    // Fake out the mouse object
    override val mouse : Mouse by lazy {
        object : Mouse by client.inputsManager.mouse {
            override fun getCursorX() = client.inputsManager.mouse.cursorX / guiScale
            override fun getCursorY() = client.inputsManager.mouse.cursorY / guiScale
        }
    }

    override fun hasFocus() = client.gameWindow.hasFocus()

    override fun localization() = client.content.localization()

    override fun openInventories(vararg inventories: Inventory) {
        val ingameClient = client.ingame ?: return logger.warn("Asked to open an inventory but the client currently isn't ingame !")

        ingameClient.ingameGuiLayer.focus(false)
        ingameClient.gui.topLayer = InventoryView(ingameClient.gui, ingameClient.ingameGuiLayer, inventories.toList())
    }
}