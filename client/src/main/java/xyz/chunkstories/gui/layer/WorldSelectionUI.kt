//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer

import org.joml.Vector4f
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.gui.*
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.LargeButtonWithIcon
import xyz.chunkstories.api.gui.elements.Scroller
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse
import xyz.chunkstories.api.input.Mouse.MouseScroll
import xyz.chunkstories.api.world.WorldInfo
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.ingame.enterExistingWorld
import xyz.chunkstories.gui.ConfirmUI
import xyz.chunkstories.util.FoldersUtils
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.WorldLoadingException
import xyz.chunkstories.world.deserializeWorldInfo
import java.io.File
import java.sql.Timestamp
import java.text.SimpleDateFormat

/** GUI for choosing a level to play SP  */
class WorldSelectionUI internal constructor(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val backOption = LargeButtonWithIcon(this, "back")
    private val newWorldOption = LargeButtonWithIcon(this, "new")

    //private val worldsButtons = ArrayList<LocalWorldButton>()

    //private var scroll = 0
    val scroller: Scroller<LocalWorldUIPanel>

    init {
        this.backOption.action = Runnable { gui.popTopLayer() }
        this.newWorldOption.action = Runnable { gui.topLayer = WorldCreationUI(gui, this@WorldSelectionUI) }

        elements.add(backOption)
        elements.add(newWorldOption)

        scroller = Scroller(this, 0, 0, emptyList())
        elements.add(scroller)

        loadWorlds()
    }

    fun loadWorlds() {
        scroller.elements.clear()

        val worldsFolder = File("." + "/worlds")
        if (!worldsFolder.exists())
            worldsFolder.mkdirs()

        val list = worldsFolder.listFiles()?.mapNotNull { worldDirectory ->
            val worldInfoFile = File(worldDirectory.absolutePath + "/" + WorldImplementation.worldPropertiesFilename)

            if (worldInfoFile.exists()) {
                val worldInfo = deserializeWorldInfo(worldInfoFile)

                LocalWorldUIPanel(0, 0, worldDirectory, worldInfo)
            } else {
                null
            }
        } ?: emptyList()

        scroller.elements.addAll(list)
    }

    override fun render(drawer: GuiDrawer) {
        var posY = gui.viewportHeight
        posY -= 24 + 4

        val titleFont = drawer.fonts.getFont("LiberationSans-Regular", 18f)
        drawer.drawStringWithShadow(titleFont, 8, posY, "#{menu.singleplayerworldselect}", -1, Vector4f(1f))

        posY -= 4

        val remainingSpace = posY - 8 - backOption.height - 8
        val buttonMargin = 8

        scroller.positionX = buttonMargin
        scroller.positionY = posY - remainingSpace
        scroller.width = gui.viewportWidth - buttonMargin * 2
        scroller.height = remainingSpace
        scroller.render(drawer)

        backOption.setPosition(8, 8)
        backOption.render(drawer)

        newWorldOption.setPosition(gui.viewportWidth - newWorldOption.width - 8, 8)
        newWorldOption.render(drawer)
    }

    override fun handleInput(input: Input): Boolean {
        if (input is MouseScroll) {
            scroller.handleScroll(input)
            return true
        }

        return super.handleInput(input)
    }

    inner class LocalWorldUIPanel
    internal constructor(x: Int, y: Int, private val directory: File, val worldInfo: WorldInfo) : GuiElement(this@WorldSelectionUI, 0, 0), ClickableGuiElement {

        val buttons = mutableListOf<Button>()

        val playButton = Button(layer, 0, 0, 0, "#{menu.play}")
        val renameButton = Button(layer, 0, 0, 0, "#{menu.rename}")
        val deleteButton = Button(layer, 0, 0, 0, "#{menu.delete}")

        init {
            playButton.action = Runnable {
                try {
                    (gui.client as ClientImplementation).enterExistingWorld(directory)
                } catch (e: WorldLoadingException) {
                    val cause = e.cause

                    gui.topLayer = MessageBoxUI(gui, layer, "Cannot load world", cause?.message ?: cause.toString())
                }
            }

            deleteButton.action = Runnable {
                gui.topLayer = ConfirmUI(gui, layer, "Do you reall want to delete '${worldInfo.name} ?'", "It will be gone forever! I heard somewhere that's a long time!") { confirmed ->
                    if (confirmed) {
                        FoldersUtils.deleteFolder(directory)
                        this@WorldSelectionUI.loadWorlds()
                    }
                }
            }

            renameButton.action = Runnable {
                gui.topLayer = WorldRenameUI(gui, layer, worldInfo, directory) {
                    this@WorldSelectionUI.loadWorlds()
                }
            }

            buttons.add(playButton)
            buttons.add(renameButton)
            buttons.add(deleteButton)
        }

        private val lastEdit: String

        init {
            val internalDataFile = File(directory.absolutePath + "/" + WorldImplementation.worldInternalDataFilename)
            lastEdit = if (internalDataFile.exists()) {
                "Last edit: " + SimpleDateFormat("yyyy-MM-dd HH:mm").format(Timestamp(internalDataFile.lastModified()))
            } else {
                "error"
            }
        }

        override fun render(drawer: GuiDrawer) {
            this.height = 64

            val font = drawer.fonts.getFont("LiberationSans-Regular", 12f)
            val fontBigue = drawer.fonts.getFont("LiberationSans-Regular", 16f)
            val fontSmale = drawer.fonts.getFont("LiberationSans-Regular", 10f)

            val texture = if (isMouseOver)
                "textures/gui/scalableButtonOver.png"
            else
                "textures/gui/scalableButton.png"

            drawer.drawBoxWithCorners(positionX, positionY, width, height, 8, texture)

            //TODO Re-implement world icons
            //ObjectRenderer.renderTexturedRect(xPosition + 32 + 4, yPosition + 32 + 4, 64, 64, GameDirectory.getGameFolderPath() + "/worlds/" + info.getInternalName() + "/worldInfo.png");
            drawer.drawBox(positionX, positionY, 64, 64, "textures/gui/icon.png", null)

            //title
            drawer.drawString(fontBigue, positionX + 72, positionY + 32 + 4, worldInfo.name, width - 72, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            //desc
            drawer.drawString(font, positionX + 72, positionY + 20, worldInfo.description, -1, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            //size
            val sizeTxt = "${worldInfo.size.sizeInChunks * 32}x${worldInfo.size.sizeInChunks * 32} blocks, ${worldInfo.generatorName} generator"
            val sizeSize = fontSmale.getWidth(sizeTxt)
            drawer.drawString(fontSmale, positionX + width - sizeSize - 4, positionY + 32 + 12, sizeTxt, width - 72, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            // last edit
            val lastEditSize = fontSmale.getWidth(lastEdit)
            drawer.drawString(fontSmale, positionX + width - lastEditSize - 4, positionY + 32 + 0, lastEdit, width - 72, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            var alignButtonsX = positionX + width
            alignButtonsX -= 8 + deleteButton.width
            deleteButton.positionX = alignButtonsX
            deleteButton.positionY = positionY + 8
            deleteButton.render(drawer)

            alignButtonsX -= 8 + renameButton.width
            renameButton.positionX = alignButtonsX
            renameButton.positionY = positionY + 8
            renameButton.render(drawer)

            alignButtonsX -= 8 + playButton.width
            playButton.positionX = alignButtonsX
            playButton.positionY = positionY + 8
            playButton.render(drawer)
        }

        override fun handleClick(mouseButton: Mouse.MouseButton): Boolean {
            //TODO doubleclick to enter?
            //this.layer.gui.client.soundManager.playSoundEffect("sounds/gui/gui_click2.ogg")
            return buttons.find { it.isMouseOver }?.handleClick(mouseButton) ?: false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("client.world")
    }
}
