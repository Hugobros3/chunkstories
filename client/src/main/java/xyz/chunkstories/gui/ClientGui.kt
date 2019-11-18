package xyz.chunkstories.gui

import org.slf4j.LoggerFactory
import xyz.chunkstories.api.gui.Fonts
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.input.lwjgl3.Lwjgl3MouseButton

val logger = LoggerFactory.getLogger("client.gui")

class ClientGui(override val client: ClientImplementation) : Gui {

    override val fonts: Fonts by lazy { FontsLibrary(client.content) }

    override var topLayer: Layer? = null

    var guiScale: Int = 1
        private set

    var guiScaleOverride = -1
        set(value) {
            field = value
            updateGuiScale()
        }

    var guiScaleUpdateHook: () -> Unit = {}

    fun updateGuiScale() {
        if (guiScaleOverride != -1) {
            guiScale = guiScaleOverride
        } else {
            val scaleHorizontal = Math.floor(client.gameWindow.width / 512.0).toInt()
            val scaleVertical = Math.floor(client.gameWindow.height / 320.0).toInt()

            //TODO make this configurable
            val minScale = Math.min(scaleHorizontal, scaleVertical)
            if (minScale in 1..16)
                guiScale = minScale
            else
                guiScale = 1
        }

        viewportWidth = client.gameWindow.width / guiScale
        viewportHeight = client.gameWindow.height / guiScale

        guiScaleUpdateHook()
    }

    override var viewportWidth: Int = 1
        private set
    override var viewportHeight: Int = 1
        private set

    // Fake out the mouse object
    override val mouse: Mouse by lazy {
        val realMouse = client.inputsManager.mouse
        object : Mouse by realMouse {
            override val cursorX: Double
                get() = client.inputsManager.mouse.cursorX / guiScale
            override val cursorY: Double
                get() = client.inputsManager.mouse.cursorY / guiScale

            //TODO do this the other way arround and have the gameplay stuff use a lying mouse object
            //to hide the 'isPressed' thing.
            private fun bypassIngameLie(realMouseButton: Mouse.MouseButton): Mouse.MouseButton {
                if (realMouseButton !is Lwjgl3MouseButton)
                    throw Exception("oh no my hacky code bit me in the arse")

                return object : Mouse.MouseButton by realMouseButton {
                    override val isPressed: Boolean
                        get() = realMouseButton.isDown

                    override val mouse: Mouse
                        get() = this@ClientGui.mouse
                }
            }

            override val mainButton: Mouse.MouseButton = bypassIngameLie(realMouse.mainButton)
            override val secondaryButton: Mouse.MouseButton = bypassIngameLie(realMouse.secondaryButton)
            override val middleButton: Mouse.MouseButton = bypassIngameLie(realMouse.middleButton)
        }
    }

    fun translateInputForGui(input: Input): Input {
        when (input) {
            client.inputsManager.mouse.mainButton -> return mouse.mainButton
            client.inputsManager.mouse.secondaryButton -> return mouse.secondaryButton
            client.inputsManager.mouse.middleButton -> return mouse.middleButton
        }
        return input
    }

    override fun hasFocus() = client.gameWindow.hasFocus()

    override fun localization() = client.content.localization()

    /*override fun openInventories(vararg inventories: Inventory) {
        val ingameClient = client.ingame ?: return logger.warn("Asked to open an inventory but the client currently isn't ingame !")

        ingameClient.ingameGuiLayer.focus(false)
        ingameClient.gui.topLayer = InventoryView(ingameClient.gui, ingameClient.ingameGuiLayer, inventories.toList())
    }*/
}