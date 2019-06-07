//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer.config

import xyz.chunkstories.api.content.mods.Mod
import xyz.chunkstories.api.exceptions.content.mods.ModLoadFailureException
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.LargeButtonWithIcon
import xyz.chunkstories.api.gui.elements.ScrollableContainer
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse
import xyz.chunkstories.api.input.Mouse.MouseButton
import xyz.chunkstories.api.input.Mouse.MouseScroll
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.content.mods.ModFolder
import xyz.chunkstories.content.mods.ModImplementation
import xyz.chunkstories.content.mods.ModZip
import xyz.chunkstories.gui.layer.config.ModsSelection.ModsScrollableContainer.ModItem
import org.joml.Vector4f
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class ModsSelection(window: Gui, parent: Layer) : Layer(window, parent) {

    private val applyMods = LargeButtonWithIcon(this, "validate")
    private val backOption = LargeButtonWithIcon(this, "back")

    private val locateExtMod = Button(this, 0, 0, "Locate external mod")
    private val openModsFolder = Button(this, 0, 0, "Open mods folder")

    private val modsContainer = ModsScrollableContainer(this)

    init {
        elements.add(modsContainer)

        elements.add(locateExtMod)
        elements.add(openModsFolder)
        elements.add(backOption)
        elements.add(applyMods)

        this.backOption.action = Runnable { gui.topLayer = parentLayer }

        this.applyMods.action = Runnable {
            val modsEnabled = ArrayList<String>()
            for (e in modsContainer.elements) {
                val modItem = e as ModItem
                if (modItem.enabled) {
                    logger.info("Adding " + (modItem.mod as ModImplementation).loadString + " to mod path")
                    modsEnabled.add((modItem.mod as ModImplementation).loadString)
                }
            }
            gui.client.content.modsManager().setEnabledMods(*modsEnabled.toTypedArray())

            (gui.client as ClientImplementation).reloadAssets()
            buildModsList()
        }

        buildModsList()
    }

    private fun buildModsList() {
        modsContainer.elements.clear()
        val currentlyEnabledMods = Arrays
                .asList(*gui.client.content.modsManager().enabledModsString)

        val uniqueMods = HashSet<String>()
        // First put in already loaded mods
        for (mod in gui.client.content.modsManager().currentlyLoadedMods) {
            // Should use md5 hash instead ;)
            if (uniqueMods.add(mod.modInfo.name.toLowerCase()))
                modsContainer.elements.add(modsContainer.ModItem(mod, true))
        }

        // Then look for mods in folder fashion
        for (f in File("." + "/mods/").listFiles()!!) {
            if (f.isDirectory) {
                val txt = File(f.absolutePath + "/mod.txt")
                if (txt.exists()) {
                    try {
                        val mod = ModFolder(f)
                        // Should use md5 hash instead ;)
                        if (uniqueMods.add(mod.modInfo.name.toLowerCase()))
                            modsContainer.elements.add(modsContainer.ModItem(mod,
                                    currentlyEnabledMods.contains(mod.modInfo.name)))

                        println("mod:" + mod.modInfo.name + " // "
                                + currentlyEnabledMods.contains(mod.modInfo.name))
                    } catch (e: ModLoadFailureException) {
                        e.printStackTrace()
                    }

                }
            }
        }
        // Then look for .zips
        // First look for mods in folder fashion
        for (f in File("." + "/mods/").listFiles()!!) {
            if (f.name.endsWith(".zip")) {
                try {
                    val mod = ModZip(f)
                    // Should use md5 hash instead ;)
                    if (uniqueMods.add(mod.modInfo.name.toLowerCase()))
                        modsContainer.elements.add(modsContainer.ModItem(mod,
                                currentlyEnabledMods.contains(mod.modInfo.name)))
                } catch (e: ModLoadFailureException) {
                    e.printStackTrace()
                }

            }
        }
    }

    override fun render(drawer: GuiDrawer) {
        var posY = gui.viewportHeight
        posY -= 24 + 4

        val font = drawer!!.fonts.getFont("LiberationSans-Regular", 18f)
        val instructions = "Select the mods you want to use"
        drawer.drawStringWithShadow(font, 8, posY, instructions, -1, Vector4f(1f))

        backOption.setPosition(xPosition + 8, 8)
        backOption.render(drawer)

        // Display buttons

        var totalLengthOfButtons = 0
        val spacing = 2 * 1

        totalLengthOfButtons += applyMods.width
        totalLengthOfButtons += spacing

        totalLengthOfButtons += locateExtMod.width
        totalLengthOfButtons += spacing

        var buttonDisplayX = gui.viewportWidth / 2 - totalLengthOfButtons / 2
        val buttonDisplayY = 8

        locateExtMod.setPosition(buttonDisplayX, buttonDisplayY)
        buttonDisplayX += locateExtMod.width + spacing
        locateExtMod.render(drawer)

        openModsFolder.setPosition(buttonDisplayX, buttonDisplayY)
        buttonDisplayX += openModsFolder.width + spacing
        openModsFolder.render(drawer)

        applyMods.setPosition(this.width - applyMods.width - 8, 8)
        buttonDisplayX += applyMods.width + spacing
        applyMods.render(drawer)

        val offsetForButtons = applyMods.positionY + applyMods.height + 8 * 1
        val offsetForHeaderText = 32 * 1
        modsContainer.setPosition((width - 480 * 1) / 2, offsetForButtons)
        modsContainer.setSize(480 * 1, height - (offsetForButtons + offsetForHeaderText))
        modsContainer.render(drawer)
    }

    override fun handleInput(input: Input): Boolean {
        if (input is MouseScroll) {
            modsContainer.scroll(input.amount() > 0)
            return true
        }

        return super.handleInput(input)
    }

    internal inner class ModsScrollableContainer(layer: Layer) : ScrollableContainer(layer) {

        override fun render(renderer: GuiDrawer) {
            super.render(renderer)

            var text = "Showing elements "

            text += scroll
            text += "-"
            text += scroll

            text += " out of " + elements.size
            val dekal = renderer.fonts.getFont("LiberationSans-Regular", 12f).getWidth(text) / 2

            renderer.drawString(renderer.fonts.getFont("LiberationSans-Regular", 12f),
                    xPosition + width / 2 - dekal, yPosition - 128, text, -1,
                    Vector4f(0.0f, 0.0f, 0.0f, 1.0f))
        }

        internal inner class ModItem(var mod: Mod, var enabled: Boolean) : ContainerElement(mod.modInfo.name, mod.modInfo.description.replace("\\n", "\n")) {

            var icon: String

            init {
                this.topRightString = mod.modInfo.version

                val asset = mod.getAssetByName("modicon.png")
                if (asset != null)
                    icon = "@" + mod.modInfo.internalName + ":modicon.png"
                else
                    icon = "nomodicon.png"
            }

            override fun handleClick(mouseButton: MouseButton): Boolean {
                val mouse = mouseButton.mouse
                if (isOverUpButton(mouse)) {
                    val indexInList = this@ModsScrollableContainer.elements.indexOf(this)
                    if (indexInList > 0) {
                        val newIndex = indexInList - 1
                        this@ModsScrollableContainer.elements.removeAt(indexInList)
                        this@ModsScrollableContainer.elements.add(newIndex, this)
                    }
                } else if (isOverDownButton(mouse)) {
                    val indexInList = this@ModsScrollableContainer.elements.indexOf(this)
                    if (indexInList < this@ModsScrollableContainer.elements.size - 1) {
                        val newIndex = indexInList + 1
                        this@ModsScrollableContainer.elements.removeAt(indexInList)
                        this@ModsScrollableContainer.elements.add(newIndex, this)
                    }
                } else if (isOverEnableDisableButton(mouse)) {
                    // TODO: Check for conflicts when enabling
                    enabled = !enabled
                } else
                    return false
                return true
            }

            fun isOverUpButton(mouse: Mouse): Boolean {
                val s = 1
                val mx = mouse.cursorX
                val my = mouse.cursorY

                val positionX = (this.positionX + 460 * s).toFloat()
                val positionY = (this.positionY + 37 * s).toFloat()
                val width = 18
                val height = 17
                return (mx >= positionX && mx <= positionX + width * s && my >= positionY
                        && my <= positionY + height * s)
            }

            fun isOverEnableDisableButton(mouse: Mouse): Boolean {
                val s = 1
                val mx = mouse.cursorX
                val my = mouse.cursorY

                val positionX = (this.positionX + 460 * s).toFloat()
                val positionY = (this.positionY + 20 * s).toFloat()
                val width = 18
                val height = 17
                return (mx >= positionX && mx <= positionX + width * s && my >= positionY
                        && my <= positionY + height * s)
            }

            fun isOverDownButton(mouse: Mouse): Boolean {
                val s = 1
                val mx = mouse.cursorX
                val my = mouse.cursorY

                val positionX = (this.positionX + 460 * s).toFloat()
                val positionY = (this.positionY + 2 * s).toFloat()
                val width = 18
                val height = 17
                return (mx >= positionX && mx <= positionX + width * s && my >= positionY
                        && my <= positionY + height * s)
            }

            override fun render(drawer: GuiDrawer) {
                val mouse = gui.mouse

                // Setup textures
                val bgTexture = if (isMouseOver(mouse)) "textures/gui/modsOver.png" else "textures/gui/mods.png"

                val upArrowTexture = "textures/gui/modsArrowUp.png"
                val downArrowTexture = "textures/gui/modsArrowDown.png"

                val enableDisableTexture = if (enabled) "textures/gui/modsDisable.png" else "textures/gui/modsEnable.png"

                // Render graphical base
                drawer.drawBox(positionX, positionY, width, height, 0f, 1f, 1f, 0f, bgTexture,
                        if (enabled) Vector4f(1.0f, 1.0f, 1.0f, 1.0f) else Vector4f(1f, 1f, 1f, 0.5f))
                if (enabled) {
                    val enabledTexture = "textures/gui/modsEnabled.png"
                    drawer.drawBox(positionX, positionY, width, height,
                            0f, 1f, 1f, 0f, enabledTexture, Vector4f(1.0f, 1.0f, 1.0f, 1.0f))
                }

                // Render subbuttons
                if (isOverUpButton(mouse))
                    drawer.drawBox(positionX, positionY, width, height,
                            0f, 1f, 1f, 0f, upArrowTexture, Vector4f(1.0f, 1.0f, 1.0f, 1.0f))
                if (isOverEnableDisableButton(mouse))
                    drawer.drawBox(positionX, positionY, width, height,
                            0f, 1f, 1f, 0f, enableDisableTexture, Vector4f(1.0f, 1.0f, 1.0f, 1.0f))
                if (isOverDownButton(mouse))
                    drawer.drawBox(positionX, positionY, width, height,
                            0f, 1f, 1f, 0f, downArrowTexture, Vector4f(1.0f, 1.0f, 1.0f, 1.0f))

                // Render icon
                drawer.drawBox(positionX + 4, positionY + 4, 64,
                        64, 0f, 1f, 1f, 0f, icon, Vector4f(1.0f, 1.0f, 1.0f, 1.0f))
                // Text !
                if (name != null)
                    drawer.drawString(
                            drawer.fonts.getFont("LiberationSans-Regular", 12f), positionX + 70,
                            positionY + 54, name!!, -1, Vector4f(0.0f, 0.0f, 0.0f, 1.0f))

                if (topRightString != null) {
                    val dekal = width - drawer.fonts.getFont("LiberationSans-Regular", 12f).getWidth(topRightString!!) - 4
                    drawer.drawString(
                            drawer.fonts.getFont("LiberationSans-Regular", 12f), positionX + dekal,
                            positionY + 54, topRightString!!, -1, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))
                }

                if (descriptionLines != null)
                    drawer.drawString(
                            drawer.fonts.getFont("LiberationSans-Regular", 12f), positionX + 70,
                            positionY + 38, descriptionLines!!, -1, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            }

        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("client.mods")
    }

}
