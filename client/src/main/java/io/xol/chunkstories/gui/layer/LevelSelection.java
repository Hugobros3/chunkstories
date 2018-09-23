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
import io.xol.chunkstories.api.gui.elements.LargeButtonWithIcon;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.client.ClientImplementation;
import io.xol.chunkstories.client.ingame.IngameClientLocalHostKt;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.world.WorldInfoUtilKt;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** GUI for choosing a level to play SP */
public class LevelSelection extends Layer {
	private final Logger logger = LoggerFactory.getLogger("client.world");

	private LargeButtonWithIcon backOption = new LargeButtonWithIcon(this, "back");
	private LargeButtonWithIcon newWorldOption = new LargeButtonWithIcon(this, "new");
	private List<LocalWorldButton> worldsButtons = new ArrayList<>();

	LevelSelection(Gui gui, Layer parent) {
		super(gui, parent);

		this.backOption.setAction(gui::popTopLayer);
		this.newWorldOption.setAction(() -> gui.setTopLayer(new LevelCreation(gui, LevelSelection.this)));

		elements.add(backOption);
		elements.add(newWorldOption);

		File worldsFolder = new File(GameDirectory.getGameFolderPath() + "/worlds");
		if (!worldsFolder.exists())
			worldsFolder.mkdirs();

		for (File worldDirectory : worldsFolder.listFiles()) {
			File worldInfoFile = new File(worldDirectory.getAbsolutePath() + "/worldInfo.dat");

			if (worldInfoFile.exists()) {
				WorldInfo worldInfo = WorldInfoUtilKt.deserializeWorldInfo(worldInfoFile);

				LocalWorldButton worldButton = new LocalWorldButton(0, 0, worldInfo);
				worldButton.setAction(() -> IngameClientLocalHostKt.enterExistingWorld((ClientImplementation) gui.getClient(), worldDirectory));

				elements.add(worldButton);
				worldsButtons.add(worldButton);

			}
		}
	}

	private int scroll = 0;

	@Override
	public void render(GuiDrawer drawer) {
		parentLayer.getRootLayer().render(drawer);

		if (scroll < 0)
			scroll = 0;

		int posY = gui.getViewportHeight() - 128;

		Font font = drawer.getFonts().getFont("LiberationSans-Regular", 33);

		drawer.drawStringWithShadow(font, 64, posY + 64, "Select a level...", -1, new Vector4f(1));

		int remainingSpace = gui.getViewportHeight() / 96 - 2;

		while (scroll + remainingSpace > worldsButtons.size())
			scroll--;

		int skip = scroll;
		for (LocalWorldButton worldButton : worldsButtons) {
			if (skip-- > 0)
				continue;
			if (remainingSpace-- <= 0)
				break;

			int maxWidth = gui.getViewportWidth() - 64 * 2;
			worldButton.setWidth(maxWidth);
			worldButton.setPosition(64, posY);
			worldButton.render(drawer);

			posY -= 96;
		}

		backOption.setPosition(8, 8);
		backOption.render(drawer);

		newWorldOption.setPosition(gui.getViewportWidth() - newWorldOption.getWidth() - 8, 8);
		newWorldOption.render(drawer);
	}

	@Override
	public boolean handleInput(Input input) {
		if (input instanceof MouseScroll) {
			MouseScroll ms = (MouseScroll) input;
			if (ms.amount() < 0)
				scroll++;
			else
				scroll--;
			return true;
		}

		return super.handleInput(input);
	}

	public class LocalWorldButton extends Button {
		public WorldInfo info;

		LocalWorldButton(int x, int y, WorldInfo info) {
			super(LevelSelection.this, x, y, "");
			this.info = info;
		}

		@Override
		public void render(GuiDrawer renderer) {
			String texture = ((isFocused() || isMouseOver()) ? "./textures/gui/scalableButtonOver.png"
							: "./textures/gui/scalableButton.png");

			this.setHeight(36);

			renderer.drawBoxWithCorners(xPosition, yPosition, width, height, 8, texture);

			//TODO redo
			//ObjectRenderer.renderTexturedRect(xPosition + 32 + 4, yPosition + 32 + 4, 64, 64, GameDirectory.getGameFolderPath() + "/worlds/" + info.getInternalName() + "/worldInfo.png");

			Font font = renderer.getFonts().getFont("LiberationSans-Regular", 22);

			renderer.drawStringWithShadow(font, xPosition + 72, yPosition + 34, info.getName() + "#CCCCCC    Size : " + info.getSize().toString() + " ( " + info.getSize().sizeInChunks / 32 + "x" + info.getSize().sizeInChunks / 32 + " km )", width - 72, new Vector4f(1.0f));
			renderer.drawStringWithShadow(font, xPosition + 72, yPosition + 4, info.getDescription(), -1, new Vector4f(1.0f));

		}

		public int getWidth() {
			return width;
		}
	}
}
