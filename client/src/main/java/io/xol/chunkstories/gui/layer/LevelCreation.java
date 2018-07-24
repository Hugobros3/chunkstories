//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import java.util.Iterator;

import org.joml.Vector4f;

import io.xol.chunkstories.api.content.Content.WorldGenerators.WorldGeneratorDefinition;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.gui.elements.InputText;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.WorldLoadingException;

/** GUI for creating new levels */
public class LevelCreation extends Layer {
	BaseButton cancelOption = new BaseButton(this, 0, 0, 75, "Cancel");
	BaseButton createOption = new BaseButton(this, 0, 0, 75, "Create");

	InputText levelName = new InputText(this, 0, 0, 100);
	InputText worldGenName = new InputText(this, 0, 0, 100);

	public LevelCreation(GameWindow scene, Layer parent) {
		super(scene, parent);

		this.cancelOption.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}
		});

		this.createOption.setAction(new Runnable() {
			@Override
			public void run() {
				WorldGeneratorDefinition worldGenerator = Client.getInstance().getContent().generators()
						.getWorldGeneratorUnsafe(worldGenName.getText());
				if (worldGenerator != null) {
					// String generator = "flat";
					String internalName = levelName.getText().replaceAll("[^\\w\\s]", "_");
					WorldInfoImplementation info = new WorldInfoImplementation(internalName, levelName.getText(),
							"" + System.currentTimeMillis(), "", WorldInfo.WorldSize.MEDIUM, worldGenName.getText());

					try {
						// WorldInfoFile.createNewWorld(new File(GameDirectory.getGameFolderPath() +
						// "/worlds/" + internalName),
						Client.getInstance().changeWorld(new WorldClientLocal(Client.getInstance(), info));
					} catch (WorldLoadingException e) {
						gameWindow.getClient().exitToMainMenu(e.getMessage());
					}
				}
			}
		});

		elements.add(cancelOption);
		elements.add(createOption);

		elements.add(levelName);
		elements.add(worldGenName);

		worldGenName.setText("flat");

		int frame_border_size = 64;

		xPosition = xPosition + frame_border_size;
		yPosition = yPosition + frame_border_size;

		width -= frame_border_size * 2;
		height -= frame_border_size * 2;
	}

	@Override
	public void render(RenderingInterface renderer) {
		if (parentLayer != null)
			this.parentLayer.render(renderer);
		int scale = gameWindow.getGuiScale();

		renderer.getGuiRenderer().drawBox(-1.0f, -1.0f, 1.0f, 1.0f, 0, 0, 0, 0, null, true, false,
				new Vector4f(0.0f, 0.0f, 0.0f, 0.25f));

		// int frame_border_size = 64;

		float positionStartX = xPosition;// + frame_border_size;
		float positionStartY = yPosition;// + frame_border_size;

		// width -= frame_border_size * 2;
		// height -= frame_border_size * 2;

		float x = positionStartX + 20;
		// int y = 48;
		renderer.getGuiRenderer().drawCorneredBoxTiled(positionStartX, positionStartY, width, height, 8,
				renderer.textures().getTexture("./textures/gui/scalableButton.png"), 32, 2);

		renderer.getFontRenderer().drawStringWithShadow(
				renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64,
				"Create a new World", 3, 3, new Vector4f(1));
		renderer.getFontRenderer().drawStringWithShadow(
				renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64 - 32,
				"For use in singleplayer", 2, 2, width, new Vector4f(1));

		renderer.getFontRenderer().drawStringWithShadow(
				renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), x,
				positionStartY + height - 64 - 96 - 4, "Level name", 2, 2, width, new Vector4f(1));
		int lvlnm_l = renderer.getFontRenderer().getFont("LiberationSans-Regular", 12).getWidth("Level name") * 2;

		levelName.setPosition(x + lvlnm_l + 20, positionStartY + height - 64 - 96);
		levelName.setWidth((width - (x + lvlnm_l + 20) - 20) / 2);
		levelName.render(renderer);

		String wg_string = "World generator to use";
		renderer.getFontRenderer().drawStringWithShadow(
				renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), x,
				positionStartY + height - 64 - 148 - 4, wg_string, 2, 2, width, new Vector4f(1));
		int wg_sl = renderer.getFontRenderer().getFont("LiberationSans-Regular", 12).getWidth(wg_string) * 2;

		worldGenName.setPosition(x + wg_sl + 20, positionStartY + height - 64 - 148);
		worldGenName.setWidth((width - (x + wg_sl + 20) - 20) / 2);
		worldGenName.render(renderer);

		WorldGeneratorDefinition wg = Client.getInstance().getContent().generators()
				.getWorldGeneratorUnsafe(worldGenName.getText());
		String wg_validity_string;
		if (wg == null) {
			wg_validity_string = "#FF0000'" + worldGenName.getText()
					+ "' wasnt found in the list of loaded world generators.";
		} else {
			wg_validity_string = "#00FF00'" + worldGenName.getText() + "' is a valid world generator !";
		}

		String wg_list = "Available world generators: ";
		Iterator<WorldGeneratorDefinition> iwg = Client.getInstance().getContent().generators().all();
		while (iwg != null && iwg.hasNext()) {
			WorldGeneratorDefinition wgt = iwg.next();
			wg_list += wgt.getName();
			if (iwg.hasNext())
				wg_list += ", ";
		}

		renderer.getFontRenderer().drawStringWithShadow(
				renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), x,
				positionStartY + height - 64 - 196 - 4, wg_validity_string, 2, 2, width, new Vector4f(1));
		renderer.getFontRenderer().drawStringWithShadow(
				renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), x,
				positionStartY + height - 64 - 196 - 4 - 32, wg_list, 2, 2, width, new Vector4f(1));

		cancelOption.setPosition(positionStartX + 20 * scale, positionStartY + 20 * scale);
		cancelOption.render(renderer);

		createOption.setPosition(positionStartX + width - createOption.getWidth() - 20 * scale,
				positionStartY + 20 * scale);
		createOption.render(renderer);
	}
}
