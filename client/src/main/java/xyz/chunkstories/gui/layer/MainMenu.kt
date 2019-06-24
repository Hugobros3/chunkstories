//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer

import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.LargeButton
import xyz.chunkstories.api.gui.elements.LargeButtonWithIcon
import xyz.chunkstories.gui.layer.config.LanguageSelectionScreen
import xyz.chunkstories.gui.layer.config.LogPolicyAsk
import xyz.chunkstories.gui.layer.config.ModsSelection
import xyz.chunkstories.gui.layer.config.OptionsScreen
import xyz.chunkstories.gui.layer.ingame.DeathScreen
import xyz.chunkstories.gui.printCopyrightNotice

/** Gives quick access to the main features of the game  */
class MainMenu(gui: Gui, parent: Layer?) : Layer(gui, parent) {
    internal var largeOnline = LargeButtonWithIcon(this, "online")
    internal var largeMods = LargeButtonWithIcon(this, "mods")

    internal var largeSingleplayer = LargeButton(this, "singleplayer")
    internal var largeOptions = LargeButton(this, "options")

    init {

        this.largeSingleplayer.action = Runnable { this.gui.topLayer = LevelSelection(this.gui, this@MainMenu) }
        this.largeOnline.action = Runnable { this.gui.topLayer = ServerSelection(this.gui, this@MainMenu, false) }
        this.largeMods.action = Runnable { this.gui.topLayer = ModsSelection(this.gui, this@MainMenu) }
        this.largeOptions.action = Runnable { this.gui.topLayer = OptionsScreen(this.gui, this@MainMenu) }

        largeOnline.width = 104
        largeSingleplayer.width = 104
        largeMods.width = 104
        largeOptions.width = 104

        elements.add(largeOnline)
        elements.add(largeMods)

        elements.add(largeSingleplayer)
        elements.add(largeOptions)
    }

    override fun render(drawer: GuiDrawer) {
        parentLayer?.render(drawer)

        if (gui.topLayer === this && gui.client.configuration.getValue(LogPolicyAsk.logPolicyConfigNode) == "ask")
            gui.topLayer = LogPolicyAsk(gui, this)

        val spacing = 4
        val buttonsAreaSize = largeSingleplayer.width * 2 + spacing

        val leftButtonX = gui.viewportWidth / 2 - buttonsAreaSize / 2

        val ySmall = 24
        val yBig = ySmall + largeSingleplayer.height + spacing

        largeOnline.setPosition(leftButtonX, yBig)
        largeOnline.render(drawer!!)

        largeSingleplayer.setPosition(leftButtonX, ySmall)
        largeSingleplayer.render(drawer)

        val rightButtonX = leftButtonX + largeSingleplayer.width + spacing

        largeMods.setPosition(rightButtonX, yBig)
        largeMods.render(drawer)

        largeOptions.setPosition(rightButtonX, ySmall)
        largeOptions.render(drawer)

        printCopyrightNotice(drawer)
    }

    override fun handleTextInput(c: Char): Boolean {
        when (c) {
            'd' -> gui.topLayer = DeathScreen(gui, this)
            'r' -> gui.topLayer = MessageBox(gui, this, "Dummy error", "Oh noes")
            'l' -> gui.topLayer = LanguageSelectionScreen(gui, this, true)
            'o' -> gui.topLayer = LogPolicyAsk(gui, this)
            /*'i' -> {
                val dummyInventory = Inventory(10, 4, null, null)

                fun makeItem(itemName: String): Item {
                    val def = gui.client.content.items().getItemDefinition(itemName)!!
                    val item = def.newItem<Item>()
                    return item
                }

                fun addItem(itemName: String, a: Int) {
                    val item = makeItem(itemName)
                    dummyInventory.addItem(item, a)
                }

                addItem("iron_shovel", 1)
                addItem("bread", 20)
                addItem("wood_door", 2)
                addItem("coal", 64)

                val inventoryUILayer = InventoryManagementUI(gui, this)
                val goldMaker = InventoryUI(inventoryUILayer, 8 + 20 + 20 + 8, 8 + 20 + 8)
                goldMaker.apply {
                    val inputSlot = InventorySlot.FakeSlot()
                    val outputSlot = object : InventorySlot.SummoningSlot() {

                        override val visibleContents: Pair<Item, Int>?
                            get() {
                                val input = inputSlot.visibleContents
                                val output = if(input == null) null else Pair(makeItem("gold_bar"), input.second)
                                return output
                            }

                        override fun commitTransfer(destinationInventory: Inventory, destX: Int, destY: Int, amount: Int) {
                            repeat(amount) {
                                val stealing = inputSlot.stealingFrom.removeAt(0)
                                stealing.stolen--
                                stealing.actualItemPile?.let { it.amount-- } ?: return
                                destinationInventory.placeItemAt(destX, destY, makeItem("gold_bar"), 1)
                            }
                        }
                    }

                    slots.add(InventorySlotUI(inputSlot, 8, 8))
                    slots.add(InventorySlotUI(outputSlot, 8 + 20, 8))
                }

                val (craftUi, slots) = dummyInventory.makeUIWithCraftingArea(inventoryUILayer, 3)
                inventoryUILayer.subwindows.add(craftUi)
                inventoryUILayer.subwindows.add(goldMaker)
                gui.topLayer = inventoryUILayer
            }*/
            'c' -> // Fabricated crash
                throw RuntimeException("Epic crash")
        }

        return super.handleTextInput(c)
    }
}
