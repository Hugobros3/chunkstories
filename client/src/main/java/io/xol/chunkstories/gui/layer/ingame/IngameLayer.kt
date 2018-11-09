//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame

import io.xol.chunkstories.api.client.LocalPlayer
import io.xol.chunkstories.api.entity.traits.serializable.TraitHealth
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory
import io.xol.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import io.xol.chunkstories.api.gui.Gui
import io.xol.chunkstories.api.gui.GuiDrawer
import io.xol.chunkstories.api.gui.Layer
import io.xol.chunkstories.api.input.Input
import io.xol.chunkstories.api.input.Mouse.MouseScroll
import io.xol.chunkstories.api.item.inventory.ItemPile
import io.xol.chunkstories.api.util.configuration.Configuration
import io.xol.chunkstories.client.InternalClientOptions
import io.xol.chunkstories.client.ingame.IngameClientImplementation
import io.xol.chunkstories.gui.InventoryGridRenderer
import io.xol.chunkstories.world.WorldClientCommon
import io.xol.chunkstories.world.WorldClientRemote

/**
 * The main layer that hosts the gameplay: renders the world, inventory and most
 * gui elements
 */
class IngameLayer(window: Gui, private val client: IngameClientImplementation) : Layer(window, null) {
    private val player: LocalPlayer
    private val world: WorldClientCommon

    // Renderer & client interface components
    private var inventoryBarDrawer: InventoryGridRenderer? = null
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
        focus(true)
    }

    fun hasFocus(): Boolean {
        return if (isCovered) false else gui.mouse.isGrabbed
    }

    override fun render(renderer: GuiDrawer) {
        var playerEntity = player.controlledEntity

        // Update the inventory previewer
        val playerInventory = playerEntity?.run { traits[TraitInventory::class] }
        if(playerInventory == null)
            inventoryBarDrawer = null
        else if(inventoryBarDrawer == null || inventoryBarDrawer?.inventory != playerInventory)
            inventoryBarDrawer = InventoryGridRenderer(playerInventory)

        // TODO MOVE MOVE MOVE
        if (playerEntity != null && playerEntity.traits.tryWithBoolean(TraitHealth::class) { this.isDead } && gui.topLayer !is DeathScreen)
            gui.topLayer = DeathScreen(gui, this)

        // Draw the GUI
        if (!guiHidden) {
            chatManager.render(renderer)

            // Draw inventory
            inventoryBarDrawer?.drawPlayerInventorySummary(renderer, gui.viewportWidth / 2, 8)

            // Draw debug info
            if (client.configuration.getBooleanValue(InternalClientOptions.showDebugInformation))
                debugInfoRendererHelper.drawDebugInfo(renderer)
            renderer.drawBox(gui.viewportWidth / 2 - 8, gui.viewportHeight / 2 - 8, 16, 16, "textures/gui/cursor.png")
        }

        // Lack of overlay should infer autofocus
        if (!isCovered)
            focus(true)

        // Check connection didn't died and change scene if it has
        if (world is WorldClientRemote) {
            if (!world.connection.isOpen)
                client.exitToMainMenu("Connection terminated (TODO betterize)")
        }

        // Auto-switch to pause if it detects the game isn't in focus anymore
        if (!client.gameWindow.hasFocus() && !isCovered) {
            focus(false)
            gui.topLayer = PauseMenu(gui, gui.topLayer)
        }
    }

    fun focus(makeInFocus: Boolean) {
        if (makeInFocus && !gui.mouse.isGrabbed) {
            client.inputsManager.mouse.isGrabbed = true
            client.inputsManager.mouse.setMouseCursorLocation((gui.viewportWidth / 2).toDouble(), (gui.viewportHeight / 2).toDouble())
        } else if (!makeInFocus)
            client.inputsManager.mouse.isGrabbed = false
    }

    override fun handleInput(input: Input): Boolean {
        val playerEntity = player.controlledEntity
        // Block inputs if chatting
        when {
            input.name == "chat" -> {
                gui.topLayer = chatManager.ChatPanelOverlay(gui, this)
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

                        if (slot > playerInventory.width)
                            false
                        else {
                            val p = playerInventory.getItemPileAt(slot, 0)
                            if (p != null)
                                slot = p.x
                            this.selectedSlot = slot
                            true
                        }
                    }
                }

                return false
            }
            input.name == "exit" -> {
                focus(false)
                guiHidden = false
                gui.topLayer = PauseMenu(gui, this)
                return true
            }
            input is MouseScroll -> if (playerEntity != null) {
                val playerInventory = playerEntity.traits[TraitInventory::class.java] ?: return false

                playerEntity.traits[TraitSelectedItem::class]?.let { esi ->
                    var selected: ItemPile?
                    var selectedInventorySlot = esi.selectedSlot

                    val originalSlot = selectedInventorySlot
                    if (input.amount() < 0) {
                        selectedInventorySlot %= playerInventory.width
                        selected = playerInventory.getItemPileAt(selectedInventorySlot, 0)
                        if (selected != null)
                            selectedInventorySlot += selected.item.definition.slotsWidth
                        else
                            selectedInventorySlot++
                    } else {
                        selectedInventorySlot--
                        if (selectedInventorySlot < 0)
                            selectedInventorySlot += playerInventory.width
                        selected = playerInventory.getItemPileAt(selectedInventorySlot, 0)
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
