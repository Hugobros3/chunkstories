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
import xyz.chunkstories.api.item.inventory.BasicInventory
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.gui.layer.config.LanguageSelectionScreen
import xyz.chunkstories.gui.layer.config.LogPolicyAsk
import xyz.chunkstories.gui.layer.config.ModsSelection
import xyz.chunkstories.gui.layer.config.OptionsScreen
import xyz.chunkstories.gui.layer.ingame.DeathScreen
import xyz.chunkstories.gui.layer.ingame.InventoryView
import xyz.chunkstories.util.VersionInfo
import org.joml.Vector4f

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

    override fun render(drawer: GuiDrawer?) {
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

        // Notices
        val noticeColor = Vector4f(1f)
        val version = "Chunk Stories Client " + VersionInfo.version
        drawer.fonts.defaultFont().getWidth(version)
        drawer.drawStringWithShadow(drawer.fonts.defaultFont(), 4, 0, version, -1, noticeColor)

        val copyrightNotice = "https://github.com/Hugobros3/chunkstories"
        val copyrightNoticeOffset = drawer.fonts.defaultFont().getWidth(copyrightNotice)
        drawer.drawStringWithShadow(drawer.fonts.defaultFont(), gui.viewportWidth - copyrightNoticeOffset - 4, 0, copyrightNotice, -1, noticeColor)
    }

    override fun handleTextInput(c: Char): Boolean {
        when (c) {
            'e' -> gui.topLayer = InventoryView(gui, this, listOf(BasicInventory(10, 4)))
            'd' -> gui.topLayer = DeathScreen(gui, this)
            'r' -> gui.topLayer = MessageBox(gui, this, "Error : error")
            'l' -> gui.topLayer = LanguageSelectionScreen(gui, this, true)
            'o' -> gui.topLayer = LogPolicyAsk(gui, this)
            'c' -> // Fabricated crash
                throw RuntimeException("Epic crash")
        }

        return super.handleTextInput(c)
    }
}
