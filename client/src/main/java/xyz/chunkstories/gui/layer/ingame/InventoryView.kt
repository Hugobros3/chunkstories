//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.ingame

import org.joml.Vector4f
import xyz.chunkstories.api.entity.EntityGroundItem
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.events.player.PlayerDropItemEvent
import xyz.chunkstories.api.events.player.PlayerMoveItemEvent
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseButton
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.item.inventory.moveTo
import xyz.chunkstories.api.net.packets.PacketInventoryMoveItemPile
import xyz.chunkstories.api.world.WorldClientNetworkedRemote
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.gui.InventoryGridRenderer

/** GUI code that handles seeing and manipulating ItemPiles in an Inventory  */
class InventoryView(gui: Gui, parent: Layer, private val inventories: List<Inventory>) : Layer(gui, parent) {
    private val drawers: List<InventoryGridRenderer> = inventories.map { InventoryGridRenderer(it) }

    companion object {
        var draggingPile: ItemPile? = null
        var draggingQuantity: Int = 0
    }

    override fun render(drawer: GuiDrawer) {
        if (parentLayer != null) {
            parentLayer!!.render(drawer)
        }

        val mouse = gui.mouse

        val margin = 4

        var totalWidth = 0
        var maxHeight = 0
        for (inv in inventories) {
            totalWidth += inv.width * 24 + margin
            maxHeight = Math.max(maxHeight, inv.height * 24)
        }
        totalWidth -= margin

        var widthAccumulation = 0
        for (i in drawers.indices) {
            val thisWidth = inventories[i].width * 24

            drawers[i].drawInventory(drawer,
                    gui.viewportWidth / 2 - totalWidth / 2 + widthAccumulation,
                    gui.viewportHeight / 2 - maxHeight / 2, false, 0, -1)

            widthAccumulation += margin + thisWidth

            // Draws the item name when highlighted
            val highlightedSlot = drawers[i].selectedSlot
            if (highlightedSlot != null) {
                val pileHighlighted = inventories[i].getItemPileAt(highlightedSlot[0], highlightedSlot[1])
                if (pileHighlighted != null) {
                    val mx = mouse.cursorX.toInt()
                    val my = mouse.cursorY.toInt()

                    drawer!!.drawStringWithShadow(drawer.fonts.defaultFont(2), mx, my, pileHighlighted.item.name, -1, Vector4f(1.0f))
                }
            }
        }

        val draggingPile = draggingPile
        if (draggingPile != null) {
            val slotSize = 24

            val width = slotSize * draggingPile.item.definition.slotsWidth
            val height = slotSize * draggingPile.item.definition.slotsHeight

            val itemIcon = draggingPile.item.getTextureName(draggingPile)
            val cx = mouse.cursorX.toInt()
            val cy = mouse.cursorY.toInt()

            drawer.drawBox(cx - 12, cy - 12, 24, 24, itemIcon)
            //selectedItem.getItem().getDefinition().getRenderer().renderItemInInventory(drawer, selectedItem, (float) mouse.getCursorX() - width / 2, (float) mouse.getCursorY() - height / 2, 2);

            if (draggingQuantity != 1)
                drawer.drawStringWithShadow(drawer.fonts.defaultFont(2),
                        cx - width / 2 + (draggingPile.item.definition.slotsWidth - 1) * slotSize,
                        cy - height / 2, draggingQuantity.toString() + "", -1, Vector4f(1f))

        }
    }

    override fun handleInput(input: Input): Boolean = when {
        input is MouseButton -> handleClick(input)
        input.name == "exit" -> {
            gui.popTopLayer()
            InventoryView.draggingPile = null
            true
        }
        else -> true
    }

    private fun handleClick(mouseButton: MouseButton): Boolean {
        // We to be ingame in order to do items manipulation
        val ingameClient = gui.client.ingame
        if (ingameClient == null) {
            gui.popTopLayer()
            draggingPile = null
            return true
        }

        val player = ingameClient.player
        val world = ingameClient.world

        for (i in drawers.indices) {
            // Close button
            if (drawers[i].isOverCloseButton) {
                gui.popTopLayer()
                draggingPile = null
            } else {
                val c = drawers[i].selectedSlot
                if (c != null) {
                    val x = c[0]
                    val y = c[1]
                    if (draggingPile == null) {
                        if (mouseButton.name == "mouse.left") {
                            draggingPile = inventories[i].getItemPileAt(x, y)
                            draggingQuantity = if (draggingPile == null) 0 else draggingPile!!.amount
                        } else if (mouseButton.name == "mouse.right") {
                            draggingPile = inventories[i].getItemPileAt(x, y)
                            draggingQuantity = if (draggingPile == null) 0 else 1
                        } else if (mouseButton.name == "mouse.middle") {
                            draggingPile = inventories[i].getItemPileAt(x, y)
                            draggingQuantity = if (draggingPile == null)
                                0
                            else
                                if (draggingPile!!.amount > 1) draggingPile!!.amount / 2 else 1
                        }
                    } else if (mouseButton.name == "mouse.right") {
                        if (draggingPile == inventories[i].getItemPileAt(x, y)) {
                            if (draggingQuantity < inventories[i].getItemPileAt(x, y)!!.amount)
                                draggingQuantity++
                        }
                    } else if (mouseButton.name == "mouse.left") {
                        // Don't try to move the item to it's current location
                        if (draggingPile!!.inventory === inventories[i] && x == draggingPile!!.x && y == draggingPile!!.y) {
                            draggingPile = null
                            return true
                        }

                        if (world is WorldMaster) {
                            val moveItemEvent = PlayerMoveItemEvent(player, draggingPile!!, inventories[i], x, y, draggingQuantity)
                            world.gameContext.pluginManager.fireEvent(moveItemEvent)

                            // If move was successfull
                            if (!moveItemEvent.isCancelled)
                                draggingPile!!.moveTo(inventories[i], x, y, draggingQuantity)
                            //draggingPile!!.moveItemPileTo(inventories[i], x, y, draggingQuantity)

                            draggingPile = null
                        } else if (world is WorldClientNetworkedRemote) {
                            // When in a remote MP scenario, send a packet
                            val packetMove = PacketInventoryMoveItemPile(world, draggingPile!!, draggingPile!!.inventory, inventories[i], draggingPile!!.x, draggingPile!!.y, x, y, draggingQuantity)
                            world.remoteServer.pushPacket(packetMove)

                            // And unsellect item
                            draggingPile = null
                        }
                    }
                    return true
                }
            }
        }

        val pile2drop = draggingPile
        // Clicked outside of any other inventory (drop!)
        if (pile2drop != null) {
            // SP scenario, replicated logic in PacketInventoryMoveItemPile
            if (world is WorldMaster) {
                // For local item drops, we need to make sure we have a sutiable entity
                val playerEntity = player.controlledEntity
                if (playerEntity != null) {

                    val loc = playerEntity.location
                    val dropItemEvent = PlayerDropItemEvent(player, pile2drop, draggingQuantity, loc)
                    world.gameContext.pluginManager.fireEvent(dropItemEvent)

                    if (!dropItemEvent.isCancelled) {
                        // If we're pulling this out of an inventory ( and not /dev/null ), we need to
                        // remove it from that
                        val sourceInventory = pile2drop.inventory

                        val entity = world.content.entities().getEntityDefinition("groundItem")!!.newEntity<EntityGroundItem>(world)
                        entity.traits[TraitInventory::class]?.inventory?.addItem(pile2drop.item, draggingQuantity)
                        loc.world.addEntity(entity)

                        pile2drop.amount -= draggingQuantity
                    }
                }
                draggingPile = null
            } else if (world is WorldClientNetworkedRemote) {
                val packetMove = PacketInventoryMoveItemPile(world, pile2drop, pile2drop.inventory, null, draggingPile!!.x, draggingPile!!.y, 0, 0, draggingQuantity)
                world.remoteServer.pushPacket(packetMove)

                draggingPile = null
            }// In MP scenario, move into /dev/null
        }

        return true

    }
}
