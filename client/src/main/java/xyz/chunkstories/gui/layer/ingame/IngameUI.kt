//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.ingame

import xyz.chunkstories.api.client.LocalIngamePlayer
import xyz.chunkstories.api.entity.traits.TraitHasOverlay
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseScroll
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.util.configuration.Configuration
import xyz.chunkstories.client.InternalClientOptions
import xyz.chunkstories.client.ingame.IngameClientImplementation
import xyz.chunkstories.gui.debug.DebugInfoRendererHelper
import xyz.chunkstories.gui.debug.FrametimesGraph
import xyz.chunkstories.gui.layer.WorldLoadingUI
import xyz.chunkstories.world.WorldClientCommon

/**
 * The main layer that hosts the gameplay: renders the world, inventory and most
 * gui elements
 */
class IngameUI(window: Gui, private val client: IngameClientImplementation) : Layer(window, null) {
    private val player: LocalIngamePlayer
    private val world: WorldClientCommon

    // Renderer & client interface components
    private val debugInfoRendererHelper: DebugInfoRendererHelper
    val chatManager: ChatManager

    // TODO: Move to config, just like f3
    private var guiHidden = false

    private val isCovered: Boolean
        get() = gui.topLayer !== this

    init {
        this.player = client.player
        this.world = client.world

        this.chatManager = ChatManager(client, this)
        this.debugInfoRendererHelper = DebugInfoRendererHelper(this)

        // Give focus
        // focus(true)
    }

    override fun render(drawer: GuiDrawer) {
        val playerEntity = player.controlledEntity

        // TODO MOVE MOVE MOVE
        if (gui.topLayer !is WorldLoadingUI && (playerEntity == null || playerEntity.traits[TraitHealth::class]?.isDead == true) && gui.topLayer !is DeathScreenUI)
            gui.topLayer = DeathScreenUI(gui, this)

        // Draw the GUI
        if (!guiHidden) {
            playerEntity?.traits?.get(TraitHasOverlay::class)?.drawEntityOverlay(drawer)
            chatManager.drawChatWindow(drawer)

            // Draw inventory
            // inventoryBarDrawer?.drawPlayerInventorySummary(drawer, gui.viewportWidth / 2, 8)

            // Draw debug info
            if (client.configuration.getBooleanValue(InternalClientOptions.showDebugInformation))
                debugInfoRendererHelper.drawDebugInfo(drawer)
            if(client.configuration.getBooleanValue(InternalClientOptions.showFrametimeGraph))
                FrametimesGraph.draw(drawer)
            drawer.drawBox(gui.viewportWidth / 2 - 8, gui.viewportHeight / 2 - 8, 16, 16, "textures/gui/cursor.png")
        }

        // Lack of overlay should infer autofocus
        if (!isCovered)
            focus(true)

        // Check connection didn't died and change scene if it has
        /*if (world is WorldClientRemote) {
            if (!world.connection.isOpen)
                client.exitToMainMenu("Connection terminated (TODO betterize)")
        }*/

        // Auto-switch to pause if it detects the game isn't in focus anymore
        if (!client.gameWindow.hasFocus() && !isCovered) {
            focus(false)
            gui.topLayer = PauseUI(gui, this)
        }
    }

    fun focus(makeInFocus: Boolean) {
        if (makeInFocus && !gui.mouse.isGrabbed) {
            client.inputsManager.mouse.isGrabbed = true
            client.inputsManager.mouse.setMouseCursorLocation((client.gameWindow.width / 2).toDouble(), (client.gameWindow.height / 2).toDouble())
        } else if (!makeInFocus)
            client.inputsManager.mouse.isGrabbed = false
    }

    override fun handleInput(input: Input): Boolean {
        val playerEntity = player.controlledEntity
        // Block inputs if chatting
        when {
            input.name == "chat" -> {
                gui.topLayer = chatManager.ChatUI(gui, this)
                focus(false)
                guiHidden = false
                return true
            }
            input.name == "hideGui" -> {
                guiHidden = !guiHidden
                return true
            }
            input.name == "screenshot" -> client.gameWindow.takeScreenshot()
            input.name == "toggleDebugInfo" -> {
                val debugInfo = client.configuration.get<Configuration.OptionBoolean>(InternalClientOptions.showDebugInformation)
                debugInfo!!.toggle()
                guiHidden = false
                return true
            }
            input.name == "takeCubemap" -> // shouldTakeACubemap = true;
                return true
            // CTRL-F12 reloads
            input.name == "reloadContent" -> {
                // Rebuild the mod FS
                client.client.reloadAssets()

                // Reload plugins
                world.pluginManager.reloadPlugins()
                return true
                // CTRL-R redraws chunks
            }
            input.name == "redrawChunks" -> //TODO
                return true
            input.name.startsWith("inventorySlot") -> {
                var requestedInventorySlot = Integer.parseInt(input.name.replace("inventorySlot", ""))
                // Match zero onto last slot
                if (requestedInventorySlot == 0)
                    requestedInventorySlot = 10

                // Map to zero-indexed inventory
                requestedInventorySlot--

                if (playerEntity != null) {
                    val playerInventory = playerEntity.traits[TraitInventory::class.java] ?: return false

                    // java lambda nonsense :(
                    val passedrequestedInventorySlot = requestedInventorySlot

                    return playerEntity.traits.tryWithBoolean<TraitSelectedItem>(TraitSelectedItem::class) {
                        // Do not accept request to select non-existent inventories slots
                        var slot = passedrequestedInventorySlot

                        if (slot > playerInventory.inventory.width)
                            false
                        else {
                            val p = playerInventory.inventory.getItemPileAt(slot, 0)
                            if (p != null)
                                slot = p.x

                            selectedSlot = slot
                            true
                        }
                    }
                }

                return false
            }
            input.name == "exit" -> {
                focus(false)
                guiHidden = false
                gui.topLayer = PauseUI(gui, this)
                return true
            }
            input is MouseScroll -> if (playerEntity != null) {
                val playerInventory = playerEntity.traits[TraitInventory::class.java] ?: return false

                playerEntity.traits[TraitSelectedItem::class]?.let { esi ->
                    var selected: ItemPile?
                    var selectedInventorySlot = esi.selectedSlot

                    val originalSlot = selectedInventorySlot
                    if (input.amount() < 0) {
                        selectedInventorySlot %= playerInventory.inventory.width
                        selected = playerInventory.inventory.getItemPileAt(selectedInventorySlot, 0)
                        if (selected != null)
                            selectedInventorySlot += selected.item.definition.slotsWidth
                        else
                            selectedInventorySlot++
                    } else {
                        selectedInventorySlot--
                        if (selectedInventorySlot < 0)
                            selectedInventorySlot += playerInventory.inventory.width
                        selected = playerInventory.inventory.getItemPileAt(selectedInventorySlot, 0)
                        if (selected != null)
                            selectedInventorySlot = selected.x
                    }

                    // Switch slot
                    if (originalSlot != selectedInventorySlot)
                        esi.selectedSlot = selectedInventorySlot
                }

                return true
            }
        }
        return false
    }
}
