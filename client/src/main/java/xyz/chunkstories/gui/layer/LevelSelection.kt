//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer

import org.joml.Vector4f
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.LargeButtonWithIcon
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.input.Mouse.MouseScroll
import xyz.chunkstories.api.world.WorldInfo
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.ingame.enterExistingWorld
import xyz.chunkstories.world.deserializeWorldInfo
import java.io.File
import java.sql.Timestamp
import java.util.*
import java.text.SimpleDateFormat

/** GUI for choosing a level to play SP  */
class LevelSelection internal constructor(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val backOption = LargeButtonWithIcon(this, "back")
    private val newWorldOption = LargeButtonWithIcon(this, "new")
    private val worldsButtons = ArrayList<LocalWorldButton>()

    private var scroll = 0

    init {
        this.backOption.action = Runnable { gui.popTopLayer() }
        this.newWorldOption.action = Runnable { gui.topLayer = LevelCreation(gui, this@LevelSelection) }

        elements.add(backOption)
        elements.add(newWorldOption)

        val worldsFolder = File("." + "/worlds")
        if (!worldsFolder.exists())
            worldsFolder.mkdirs()

        for (worldDirectory in worldsFolder.listFiles()!!) {
            val worldInfoFile = File(worldDirectory.absolutePath + "/worldInfo.dat")

            if (worldInfoFile.exists()) {
                val worldInfo = deserializeWorldInfo(worldInfoFile)

                val worldButton = LocalWorldButton(0, 0, worldDirectory, worldInfo)
                worldButton.action = Runnable { (gui.client as ClientImplementation).enterExistingWorld(worldDirectory) }

                elements.add(worldButton)
                worldsButtons.add(worldButton)
            }
        }
    }

    override fun render(drawer: GuiDrawer) {
        if (scroll < 0)
            scroll = 0

        var posY = gui.viewportHeight
        posY -= 24 + 4

        val titleFont = drawer.fonts.getFont("LiberationSans-Regular", 18f)
        drawer.drawStringWithShadow(titleFont, 8, posY, "Select a level...", -1, Vector4f(1f))

        val localWorldButtonHeight = 64 + 8
        posY -= 4

        var remainingSpace = posY - 8 - backOption.height

        //Keep the scrolling into visible stuff
        val fittableEntries = 1 + (remainingSpace - 64) / localWorldButtonHeight
        while (scroll + fittableEntries > worldsButtons.size)
            scroll--

        var skip = scroll
        for (worldButton in worldsButtons) {
            if (skip-- > 0)
                continue

            if (remainingSpace < worldButton.height) {
                break
            }

            posY -= 64

            val buttonMargin = 8
            val maxWidth = gui.viewportWidth - buttonMargin * 2

            worldButton.width = maxWidth
            worldButton.setPosition(buttonMargin, posY)
            worldButton.render(drawer)

            remainingSpace -= localWorldButtonHeight

            posY -= 8
        }

        backOption.setPosition(8, 8)
        backOption.render(drawer)

        newWorldOption.setPosition(gui.viewportWidth - newWorldOption.width - 8, 8)
        newWorldOption.render(drawer)
    }

    override fun handleInput(input: Input): Boolean {
        if (input is MouseScroll) {
            if (input.amount() < 0)
                scroll++
            else
                scroll--
            return true
        }

        return super.handleInput(input)
    }

    inner class LocalWorldButton
    internal constructor(x: Int, y: Int,
                         val directory: File, val info: WorldInfo) : Button(this@LevelSelection, x, y, "") {

        val lastEdit: String

        init {
            val worldInfoFile = File(directory.absolutePath + "/internal.dat")
            lastEdit = if(worldInfoFile != null) {
                "Last edit: "+SimpleDateFormat("yyyy-MM-dd HH:mm").format(Timestamp(worldInfoFile.lastModified()))
            } else {
                "error"
            }
        }

        override fun render(drawer: GuiDrawer) {
            val texture = if (isFocused || isMouseOver)
                "textures/gui/scalableButtonOver.png"
            else
                "textures/gui/scalableButton.png"

            this.height = 64

            drawer.drawBoxWithCorners(positionX, positionY, width, height, 8, texture)

            //TODO redo
            //ObjectRenderer.renderTexturedRect(xPosition + 32 + 4, yPosition + 32 + 4, 64, 64, GameDirectory.getGameFolderPath() + "/worlds/" + info.getInternalName() + "/worldInfo.png");
            drawer.drawBox(positionX, positionY, 64, 64, "textures/gui/icon.png", null)

            val font = drawer.fonts.getFont("LiberationSans-Regular", 12f)
            val fontBigue = drawer.fonts.getFont("LiberationSans-Regular", 16f)
            val fontSmale = drawer.fonts.getFont("LiberationSans-Regular", 10f)

            //title
            drawer.drawString(fontBigue, positionX + 72, positionY + 32 + 4, info.name, width - 72, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            //desc
            drawer.drawString(font, positionX+72, positionY+20, info.description, -1, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            //size
            val sizeTxt = "${info.size.sizeInChunks * 32}x${info.size.sizeInChunks * 32} blocks (${info.size.name.toLowerCase().capitalize()} size)"
            val sizeSize = fontSmale.getWidth(sizeTxt)
            drawer.drawString(fontSmale, positionX + width - sizeSize - 4, positionY + 32 + 12, sizeTxt, width - 72, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))

            val lastEditSize = fontSmale.getWidth(lastEdit)
            drawer.drawString(fontSmale, positionX + width - lastEditSize - 4, positionY + 32 + 0, lastEdit, width - 72, Vector4f(0.25f, 0.25f, 0.25f, 1.0f))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger("client.world")
    }
}
