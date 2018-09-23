//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

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

        int frameBorderSize = 64;

        xPosition = xPosition + frameBorderSize;
        yPosition = yPosition + frameBorderSize;

        width -= frameBorderSize * 2;
        height -= frameBorderSize * 2;
    }

    @Override
    public void render(GuiDrawer drawer) {
        if (parentLayer != null)
            this.parentLayer.render(drawer);
        int scale = 1;

        drawer.drawBox(0, 0, gui.getViewportWidth(), gui.getViewportHeight(), new Vector4f(0.0f, 0.0f, 0.0f, 0.25f));

        int positionStartX = xPosition;
        int positionStartY = yPosition;

        int paddedStartX = positionStartX + 20;
        drawer.drawBoxWithCorners(positionStartX, positionStartY, width, height, 8,
                "textures/gui/scalableButton.png");

        drawer.drawStringWithShadow(
                drawer.getFonts().getFont("LiberationSans-Regular", 36), paddedStartX, positionStartY + height - 64,
                "Create a new World", -1, new Vector4f(1));
        drawer.drawStringWithShadow(
                drawer.getFonts().getFont("LiberationSans-Regular", 24), paddedStartX, positionStartY + height - 64 - 32,
                "For use in singleplayer", -1, new Vector4f(1));

        drawer.drawStringWithShadow(drawer.getFonts().getFont("LiberationSans-Regular", 24), paddedStartX, positionStartY + height - 64 - 96 - 4, "Level name", -1, new Vector4f(1));
        int lvlnm_l = gui.getFonts().getFont("LiberationSans-Regular", 12).getWidth("Level name") * 2;

        levelName.setPosition(paddedStartX + lvlnm_l + 20, positionStartY + height - 64 - 96);
        levelName.setWidth((width - (paddedStartX + lvlnm_l + 20) - 20) / 2);
        levelName.render(drawer);

        String worldGeneratorLabel = "World generator to use";
        drawer.drawStringWithShadow(
                drawer.getFonts().getFont("LiberationSans-Regular", 24), paddedStartX,
                positionStartY + height - 64 - 148 - 4, worldGeneratorLabel, -1, new Vector4f(1));
        
        int worldGeneratorLabelOffset = drawer.getFonts().getFont("LiberationSans-Regular", 12).getWidth(worldGeneratorLabel) * 2;

        worldGenName.setPosition(paddedStartX + worldGeneratorLabelOffset + 20, positionStartY + height - 64 - 148);
        worldGenName.setWidth((width - (paddedStartX + worldGeneratorLabelOffset + 20) - 20) / 2);
        worldGenName.render(drawer);

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

        drawer.drawStringWithShadow(
                drawer.getFonts().getFont("LiberationSans-Regular", 24), paddedStartX,
                positionStartY + height - 64 - 196 - 4, worldGeneratorValidtyLabel, -1, new Vector4f(1));
        drawer.drawStringWithShadow(
                drawer.getFonts().getFont("LiberationSans-Regular", 24), paddedStartX,
                positionStartY + height - 64 - 196 - 4 - 32, worldGeneratorsAvailableLabel.toString(), -1, new Vector4f(1));

        cancelOption.setPosition(positionStartX + 20 * scale, positionStartY + 20 * scale);
        cancelOption.render(drawer);

        createOption.setPosition(positionStartX + width - createOption.getWidth() - 20 * scale,
                positionStartY + 20 * scale);
        createOption.render(drawer);
    }
}
