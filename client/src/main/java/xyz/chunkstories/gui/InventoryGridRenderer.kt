//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui

import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.gui.layer.ingame.InventoryView
import org.joml.Vector4f


/**
 * Helps with rendering the inventory grid
 */
//TODO refactor into a neater component with width and shit
class InventoryGridRenderer(val inventory: Inventory) {

    var selectedSlot: IntArray? = null
        private set
    var isOverCloseButton = false
        private set

    fun drawPlayerInventorySummary(drawer: GuiDrawer, x: Int, y: Int) {
        var selectedSlot = -1
        if (inventory is TraitInventory) {
            val esi = inventory.entity.traits[TraitSelectedItem::class.java]
            if (esi != null)
                selectedSlot = esi.selectedSlot
        }

        drawInventory(drawer, x - slotsWidth(inventory.width) / 2, y /*- slotsHeight(getInventory().getHeight(), true, 0) / 2*/, true, 0, selectedSlot)
    }

    //TODO move to own layer
    fun drawInventory(drawer: GuiDrawer, x: Int, y: Int, summary: Boolean, blankLines: Int, highlightSlot: Int) {
        val mouse = drawer.gui.mouse

        val cornerSize = 8
        val internalWidth = inventory.width * 24

        val height = if (summary) 1 else inventory.height

        val internalHeight = (height + (if (summary) 0 else 1) + blankLines) * 24
        val slotSize = 24

        val inventoryTexture = "./textures/gui/inventory/inventory.png"

        val color = Vector4f(1f, 1f, 1f, if (summary) 0.5f else 1f)
        // All 8 corners
        drawer.drawBox(x, y + internalHeight + cornerSize, cornerSize,
                cornerSize, 0f, 0.03125f, 0.03125f, 0f, inventoryTexture, color)
        drawer.drawBox(x + cornerSize, y + internalHeight + cornerSize,
                internalWidth, cornerSize, 0.03125f, 0.03125f, 0.96875f, 0f, inventoryTexture, color)
        drawer.drawBox(x + cornerSize + internalWidth,
                y + internalHeight + cornerSize, cornerSize, cornerSize, 0.96875f, 0.03125f, 1f, 0f, inventoryTexture,
                color)
        drawer.drawBox(x, y, cornerSize, cornerSize, 0f, 1f, 0.03125f, 248 / 256f,
                inventoryTexture, color)
        drawer.drawBox(x + cornerSize, y, internalWidth, cornerSize, 0.03125f,
                1f, 0.96875f, 248 / 256f, inventoryTexture, color)
        drawer.drawBox(x + cornerSize + internalWidth, y, cornerSize, cornerSize,
                0.96875f, 1f, 1f, 248 / 256f, inventoryTexture, color)
        drawer.drawBox(x, y + cornerSize, cornerSize, internalHeight, 0f,
                248f / 256f, 0.03125f, 8f / 256f, inventoryTexture, color)
        drawer.drawBox(x + cornerSize + internalWidth, y + cornerSize,
                cornerSize, internalHeight, 248 / 256f, 248f / 256f, 1f, 8f / 256f, inventoryTexture, color)
        // Actual inventory slots
        var sumSlots2HL = 0
        selectedSlot = null
        for (i in 0 until inventory.width) {
            for (j in 0 until height) {
                val mouseOver = (mouse.cursorX > x + cornerSize + i * slotSize
                        && mouse.cursorX <= x + cornerSize + i * slotSize + slotSize
                        && mouse.cursorY > y + cornerSize + j * slotSize
                        && mouse.cursorY <= y + cornerSize + j * slotSize + slotSize)
                // Just a dirt hack to always keep selected slot values where we want them
                if (mouseOver && selectedSlot == null) {
                    selectedSlot = intArrayOf(i, j)
                }

                var selectedPile: ItemPile? = null
                if (selectedSlot != null)
                    selectedPile = inventory.getItemPileAt(selectedSlot!![0], selectedSlot!![1])
                val thisPile = inventory.getItemPileAt(i, j)

                if (summary) {
                    val summaryBarSelected = inventory.getItemPileAt(highlightSlot, 0)
                    if (summaryBarSelected != null && i == summaryBarSelected.x) {
                        sumSlots2HL = summaryBarSelected.item.definition.slotsWidth
                    }
                    if (sumSlots2HL > 0 || summaryBarSelected == null && highlightSlot == i) {
                        sumSlots2HL--
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f,
                                152 / 256f, inventoryTexture, color)
                    } else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f,
                                152 / 256f, inventoryTexture, color)

                } else {
                    if (mouseOver || (selectedPile != null && thisPile != null && selectedPile.x == thisPile.x
                                    && selectedPile.y == thisPile.y)) {
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 32f / 256f, 176 / 256f, 56 / 256f,
                                152 / 256f, inventoryTexture, color)
                    } else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 176 / 256f, 32f / 256f,
                                152 / 256f, inventoryTexture, color)

                }
            }
        }

        // Blank part ( usefull for special inventories, ie player )
        for (j in inventory.height until inventory.height + blankLines) {
            for (i in 0 until inventory.width) {
                if (j == inventory.height) {
                    if (i == inventory.width - 1)
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 152 / 256f, 248 / 256f,
                                128 / 256f, inventoryTexture, color)
                    else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 152 / 256f, 32f / 256f,
                                128 / 256f, inventoryTexture, color)
                } else {
                    if (i == inventory.width - 1)
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 224f / 256f, 56 / 256f, 248 / 256f,
                                32 / 256f, inventoryTexture, color)
                    else
                        drawer.drawBox(x + cornerSize + i * slotSize,
                                y + cornerSize + j * slotSize, slotSize, slotSize, 8f / 256f, 56 / 256f, 32f / 256f,
                                32 / 256f, inventoryTexture, color)
                }
            }
        }
        // Top part
        if (!summary) {
            drawer.drawBox(x + cornerSize,
                    y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 8f / 256f, 32f / 256f, 32f / 256f,
                    8f / 256f, inventoryTexture, color)

            for (i in 1 until inventory.width - 2) {
                drawer.drawBox(x + cornerSize + i * slotSize,
                        y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 32f / 256f, 32f / 256f,
                        56f / 256f, 8f / 256f, inventoryTexture, color)
            }
            drawer.drawBox(
                    x + cornerSize + (inventory.width - 2) * slotSize,
                    y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 200f / 256f, 32f / 256f, 224 / 256f,
                    8f / 256f, inventoryTexture, color)
            isOverCloseButton = (mouse.cursorX > x + cornerSize + (inventory.width - 1) * slotSize
                    && mouse.cursorX <= x + cornerSize + (inventory.width - 1) * slotSize + slotSize
                    && mouse.cursorY > y + cornerSize + internalHeight - slotSize
                    && mouse.cursorY <= y + cornerSize + internalHeight)

            drawer.drawBox(
                    x + cornerSize + (inventory.width - 1) * slotSize,
                    y + cornerSize + internalHeight - slotSize, slotSize, slotSize, 224f / 256f, 32f / 256f,
                    248f / 256f, 8f / 256f, inventoryTexture, color)

            drawer.drawStringWithShadow(
                    drawer.fonts.getFont("LiberationSans-Bold", 12f), x + cornerSize + 6,
                    y + cornerSize + internalHeight - slotSize + 2 * 1, inventory.inventoryName, -1, Vector4f(1f, 1f, 1f, 1f))
        }


        // Draw the actual items
        for (pile in inventory) {
            val i = pile!!.x
            val j = pile.y
            if (!summary || j == 0) {
                val iconSize = 16
                val center = if (summary) slotSize * (pile.item.definition.slotsHeight - 1) / 2 else 0

                // TODO just icons now.
                val itemIcon = pile.item.getTextureName(pile)
                drawer.drawBox(x + cornerSize + i * slotSize + 4, y + cornerSize + j * slotSize + 4,
                        iconSize, iconSize, itemIcon)
                //pile.getItem().getDefinition().getRenderer().renderItemInInventory(renderer, pile,
                //        x + cornerSize + i * slotSize, y - center + cornerSize + j * slotSize, scale);
            }
        }

        // Draws the item's text ( done later to allow fontRenderer to pool their draws )
        for (pile in inventory) {
            val i = pile.x
            val j = pile.y

            if (!summary || j == 0) {
                var amountToDisplay = pile.amount

                // If we selected this item
                if (InventoryView.draggingPile != null && InventoryView.draggingPile!!.inventory != null
                        && inventory == InventoryView.draggingPile!!.inventory
                        && InventoryView.draggingPile!!.x == i && InventoryView.draggingPile!!.y == j) {
                    amountToDisplay -= InventoryView.draggingQuantity
                }

                // Draw amount of items in the pile
                if (amountToDisplay > 1)
                    drawer.drawStringWithShadow(drawer.fonts.defaultFont(),
                            x + cornerSize + (pile.item.definition.slotsWidth - 1 + i) * slotSize,
                            y + cornerSize + j * slotSize, amountToDisplay.toString() + "", -1,
                            Vector4f(1f, 1f, 1f, 1f))

                /*drawer.drawStringWithShadow(drawer.fonts.defaultFont(),
                        x + cornerSize + (pile.item.definition.slotsWidth - 1 + i) * slotSize,
                        y + cornerSize + j * slotSize, pile.item.getTextureName(pile), -1,
                        Vector4f(1f, 1f, 1f, 1f))*/
            }
        }
    }

    fun slotsWidth(slots: Int): Int {
        return 8 + slots * 24
    }

    fun slotsHeight(slots: Int, summary: Boolean, blankLines: Int): Int {
        return 8 + (slots + (if (summary) 0 else 1) + blankLines) * 24
    }
}
