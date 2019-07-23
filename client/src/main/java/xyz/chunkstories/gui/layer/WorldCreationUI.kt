//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui.layer

import xyz.chunkstories.api.gui.Font
import xyz.chunkstories.api.gui.Gui
import xyz.chunkstories.api.gui.GuiDrawer
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.elements.Button
import xyz.chunkstories.api.gui.elements.InputText
import xyz.chunkstories.api.world.WorldInfo
import xyz.chunkstories.api.world.WorldSize
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import xyz.chunkstories.client.ClientImplementation
import xyz.chunkstories.client.ingame.*
import org.joml.Vector4f

import java.io.File

/**
 * GUI for creating new levels
 */
class WorldCreationUI internal constructor(gui: Gui, parent: Layer) : Layer(gui, parent) {
    private val cancelOption = Button(this, 0, 0, 75, "Cancel")
    private val createOption = Button(this, 0, 0, 75, "Create")

    private val levelName = InputText(this, 0, 0, 100)
    private val worldGenName = InputText(this, 0, 0, 100)

    init {

        this.cancelOption.action = Runnable { gui.popTopLayer() }

        this.createOption.action = Runnable {
            //Escapes the unused characters
            val internalName = levelName.text.replace("[^\\w\\s]".toRegex(), "_")

            val worldInfo = WorldInfo(internalName, levelName.text, "Player-generated world", "" + System.currentTimeMillis(), WorldSize.MEDIUM, worldGenName.text)
            (gui.client as ClientImplementation).createAndEnterWorld(File("./worlds/$internalName/"), worldInfo)
        }

        elements.add(cancelOption)
        elements.add(createOption)

        elements.add(levelName)
        elements.add(worldGenName)

        worldGenName.text = "flat"
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

        drawer.drawBoxWithCorners(positionStartX, positionStartY, width, height, 8,
                "textures/gui/scalableButton.png")

        val paddedStartX = positionStartX + margin
        var y = positionStartY + height

        y -= 32
        drawer.drawStringWithShadow(titleFont, paddedStartX, y, "Create a new World", -1, titleColor)

        y -= 16
        drawer.drawString(textFont, paddedStartX, y, "For use in singleplayer", -1, textColor)

        y -= 48
        drawer.drawString(textFont, paddedStartX, y, "Level name", -1, textColor)
        val levelNameLabelWidth = textFont.getWidth("Level name")

        levelName.setPosition(paddedStartX + levelNameLabelWidth + textButtonMargin, y)
        levelName.width = width - (levelNameLabelWidth + margin * 2 + textButtonMargin)
        levelName.render(drawer)

        y -= 24
        val worldGeneratorLabel = "World generator to use"
        drawer.drawString(textFont, paddedStartX, y, worldGeneratorLabel, -1, textColor)
        val generatorLabelWidth = textFont.getWidth(worldGeneratorLabel)

        worldGenName.setPosition(paddedStartX + generatorLabelWidth + textButtonMargin, y)
        worldGenName.width = width - (generatorLabelWidth + margin * 2 + textButtonMargin)
        worldGenName.render(drawer)

        y -= 24
        val worldGeneratorDefinition = gui.client.content.generators.getWorldGenerator(worldGenName.text)
        val worldGeneratorValidtyLabel: String
        if (worldGeneratorDefinition.name != worldGenName.text) {
            worldGeneratorValidtyLabel = "#FF0000'" + worldGenName.text + "' wasnt found in the list of loaded world generators."
        } else {
            worldGeneratorValidtyLabel = "#00FF00'" + worldGenName.text + "' is a valid world generator !"
        }
        val worldGeneratorsAvailableLabel = "Available world generators: "+gui.client.content.generators.all.map { it.name }

        drawer.drawString(textFont, paddedStartX, y, worldGeneratorValidtyLabel, -1, textColor)

        y -= 24
        drawer.drawString(textFont, paddedStartX, y, worldGeneratorsAvailableLabel.toString(), -1, textColor)

        cancelOption.setPosition(positionStartX + margin, positionStartY + margin)
        cancelOption.render(drawer)
        createOption.setPosition(positionStartX + width - createOption.width - margin,
                positionStartY + margin)
        createOption.render(drawer)
    }
}
