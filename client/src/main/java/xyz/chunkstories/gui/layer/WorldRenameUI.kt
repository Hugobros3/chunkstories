package xyz.chunkstories.gui.layer

import org.joml.Vector4f
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.InputText
import xyz.chunkstories.api.world.WorldInfo
import xyz.chunkstories.world.WorldImplementation
import xyz.chunkstories.world.serializeWorldInfo
import java.io.File

class WorldRenameUI internal constructor(gui: Gui, parent: Layer, val worldInfo: WorldInfo, val directory: File, val callback: () -> Unit) : Layer(gui, parent) {
    private val cancelOption = Button(this, 0, 0, 75, "#{menu.cancel}")
    private val renameOption = Button(this, 0, 0, 75, "#{menu.rename}")

    private val levelName = InputText(this, 0, 0, 100)

    init {
        levelName.text = worldInfo.name

        this.cancelOption.action = Runnable { gui.popTopLayer() }

        this.renameOption.action = Runnable {
            //Escapes the unused characters
            val internalName = levelName.text.replace("[^\\w\\s]".toRegex(), "_")

            val newWorldInfo = worldInfo.copy(name = levelName.text)
            val worldInfoFile = File(directory.path + "/" + WorldImplementation.worldPropertiesFilename)
            worldInfoFile.writeText(serializeWorldInfo(newWorldInfo, true))
            gui.popTopLayer()
            callback()
            //val worldInfo = WorldInfo(internalName, levelName.text, "Player-generated world", "" + System.currentTimeMillis(), WorldSize.MEDIUM, worldGenName.text)
            //(gui.client as ClientImplementation).createAndEnterWorld(File("./worlds/$internalName/"), worldInfo)
        }

        elements.add(cancelOption)
        elements.add(renameOption)

        elements.add(levelName)
    }

    override fun render(drawer: GuiDrawer) {
        this.parentLayer?.render(drawer)

        // Resize the window every frame to partially cover the rest
        val frameBorderSize = 32

        xPosition = frameBorderSize
        yPosition = frameBorderSize

        width = gui.viewportWidth - frameBorderSize * 2
        height = gui.viewportHeight - frameBorderSize * 2

        drawer.drawBox(0, 0, gui.viewportWidth, gui.viewportHeight, Vector4f(0.0f, 0.0f, 0.0f, 0.25f))

        // Text constants
        val titleFont = drawer.fonts.getFont("LiberationSans-Regular", 24f)
        val textFont = drawer.fonts.getFont("LiberationSans-Regular", 12f)
        val titleColor = Vector4f(1f)
        //Vector4f textColor = new Vector4f(0.25f, 0.25f, 0.25f, 1f);
        val textColor = Vector4f(0f, 0f, 0f, 1f)

        val margin = 4
        val textButtonMargin = 4

        val positionStartX = xPosition
        val positionStartY = yPosition

        drawer.drawBoxWithCorners(positionStartX, positionStartY, width, height, 8, "textures/gui/scalableButton.png")

        val paddedStartX = positionStartX + margin
        var y = positionStartY + height

        y -= 32
        drawer.drawStringWithShadow(titleFont, paddedStartX, y, "Rename an existing World", -1, titleColor)

        y -= 48
        drawer.drawString(textFont, paddedStartX, y, "World name", -1, textColor)
        val levelNameLabelWidth = textFont.getWidth("World name")

        levelName.setPosition(paddedStartX + levelNameLabelWidth + textButtonMargin, y)
        levelName.width = width - (levelNameLabelWidth + margin * 2 + textButtonMargin)
        levelName.render(drawer)

        cancelOption.setPosition(positionStartX + margin, positionStartY + margin)
        cancelOption.render(drawer)
        renameOption.setPosition(positionStartX + width - renameOption.width - margin, positionStartY + margin)
        renameOption.render(drawer)
    }
}
