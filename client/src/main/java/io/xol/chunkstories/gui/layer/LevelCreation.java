//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import io.xol.chunkstories.api.gui.Font;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.Button;
import io.xol.chunkstories.api.gui.elements.InputText;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.api.world.WorldSize;
import io.xol.chunkstories.api.world.generator.WorldGeneratorDefinition;
import io.xol.chunkstories.client.ClientImplementation;
import io.xol.chunkstories.client.ingame.IngameClientLocalHostKt;
import org.joml.Vector4f;

import java.io.File;
import java.util.Iterator;

/**
 * GUI for creating new levels
 */
public class LevelCreation extends Layer {
    private Button cancelOption = new Button(this, 0, 0, 75, "Cancel");
    private Button createOption = new Button(this, 0, 0, 75, "Create");

    private InputText levelName = new InputText(this, 0, 0, 100);
    private InputText worldGenName = new InputText(this, 0, 0, 100);

    LevelCreation(Gui gui, Layer parent) {
        super(gui, parent);

        this.cancelOption.setAction(gui::popTopLayer);

        this.createOption.setAction(() -> {
            //Escapes the unused characters
            String internalName = levelName.getText().replaceAll("[^\\w\\s]", "_");

            WorldInfo worldInfo = new WorldInfo(internalName, levelName.getText(), "" + System.currentTimeMillis(), "", WorldSize.MEDIUM, worldGenName.getText());
            IngameClientLocalHostKt.createAndEnterWorld((ClientImplementation) gui.getClient(), new File("./worlds/"+internalName+"/"), worldInfo);
        });

        elements.add(cancelOption);
        elements.add(createOption);

        elements.add(levelName);
        elements.add(worldGenName);

        worldGenName.setText("flat");
    }

    @Override
    public void render(GuiDrawer drawer) {
        if (parentLayer != null)
            this.parentLayer.render(drawer);

        // Resize the window every frame to partially cover the rest
        int frameBorderSize = 32;

        xPosition = frameBorderSize;
        yPosition = frameBorderSize;

        width = gui.getViewportWidth() - frameBorderSize * 2;
        height = gui.getViewportHeight() - frameBorderSize * 2;

        drawer.drawBox(0, 0, gui.getViewportWidth(), gui.getViewportHeight(), new Vector4f(0.0f, 0.0f, 0.0f, 0.25f));

        // Text constants
        Font titleFont = drawer.getFonts().getFont("LiberationSans-Regular", 24);
        Font textFont = drawer.getFonts().getFont("LiberationSans-Regular", 12);
        Vector4f titleColor = new Vector4f(1f);
        //Vector4f textColor = new Vector4f(0.25f, 0.25f, 0.25f, 1f);
        Vector4f textColor = new Vector4f(0f, 0f, 0f, 1f);

        int margin = 4;
        int textButtonMargin = 4;

        int positionStartX = xPosition;
        int positionStartY = yPosition;

        drawer.drawBoxWithCorners(positionStartX, positionStartY, width, height, 8,
                "textures/gui/scalableButton.png");

        int paddedStartX = positionStartX + margin;
        int y = positionStartY + height;

        y -= 32;
        drawer.drawStringWithShadow(titleFont, paddedStartX, y, "Create a new World", -1, titleColor);

        y -= 16;
        drawer.drawString(textFont, paddedStartX, y, "For use in singleplayer", -1, textColor);

        y -= 48;
        drawer.drawString(textFont, paddedStartX, y, "Level name", -1, textColor);
        int levelNameLabelWidth = textFont.getWidth("Level name");

        levelName.setPosition(paddedStartX + levelNameLabelWidth + textButtonMargin, y);
        levelName.setWidth((width - (levelNameLabelWidth + margin * 2 + textButtonMargin)));
        levelName.render(drawer);

        y -= 24;
        String worldGeneratorLabel = "World generator to use";
        drawer.drawString(textFont, paddedStartX,y, worldGeneratorLabel, -1, textColor);
        int generatorLabelWidth = textFont.getWidth(worldGeneratorLabel);

        worldGenName.setPosition(paddedStartX + generatorLabelWidth + textButtonMargin, y);
        worldGenName.setWidth((width - (generatorLabelWidth + margin * 2 + textButtonMargin)));
        worldGenName.render(drawer);

        y -= 24;
        WorldGeneratorDefinition worldGeneratorDefinition = gui.getClient().getContent().generators().getWorldGenerator(worldGenName.getText());
        String worldGeneratorValidtyLabel;
        if (!worldGeneratorDefinition.getName().equals(worldGenName.getText())) {
            worldGeneratorValidtyLabel = "#FF0000'" + worldGenName.getText() + "' wasnt found in the list of loaded world generators.";
        } else {
            worldGeneratorValidtyLabel = "#00FF00'" + worldGenName.getText() + "' is a valid world generator !";
        }
        StringBuilder worldGeneratorsAvailableLabel = new StringBuilder("Available world generators: ");
        Iterator<WorldGeneratorDefinition> worldGeneratorDefinitionIterator = gui.getClient().getContent().generators().all();
        while (worldGeneratorDefinitionIterator.hasNext()) {
            WorldGeneratorDefinition wgt = worldGeneratorDefinitionIterator.next();
            worldGeneratorsAvailableLabel.append(wgt.getName());
            if (worldGeneratorDefinitionIterator.hasNext())
                worldGeneratorsAvailableLabel.append(", ");
        }

        drawer.drawString(textFont, paddedStartX, y, worldGeneratorValidtyLabel, -1, textColor);

        y -= 24;
        drawer.drawString(textFont, paddedStartX, y, worldGeneratorsAvailableLabel.toString(), -1, textColor);

        cancelOption.setPosition(positionStartX + margin, positionStartY + margin);
        cancelOption.render(drawer);
        createOption.setPosition(positionStartX + width - createOption.getWidth() - margin,
                positionStartY + margin);
        createOption.render(drawer);
    }
}
