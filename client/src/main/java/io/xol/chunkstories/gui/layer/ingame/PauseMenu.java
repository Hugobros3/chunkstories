//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame;

import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.gui.Font;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.gui.layer.MainMenu;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.Button;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.gui.layer.config.ModsSelection;
import io.xol.chunkstories.gui.layer.config.OptionsScreen;

/** The GUI code for the basic pause menu you bring about by pressing ESC */
public class PauseMenu extends Layer {
	private Button resumeButton = new Button(this, 0, 0, 160, "#{menu.resume}");
	private Button optionsButton = new Button(this, 0, 0, 160, "#{menu.options}");
	private Button modsButton = new Button(this, -100, 0, 160, "#{menu.mods}");
	private Button exitButton = new Button(this, 0, 0, 160, "#{menu.backto}");

	PauseMenu(Gui gui, Layer parent) {
		super(gui, parent);

		this.resumeButton.setAction(gui::popTopLayer);
		this.optionsButton.setAction(() -> gui.setTopLayer(new OptionsScreen(gui, PauseMenu.this)));
		this.modsButton.setAction(() -> gui.setTopLayer(new ModsSelection(gui, PauseMenu.this)));
		this.exitButton.setAction(() -> gui.getClient().getIngame().exitToMainMenu());

		elements.add(resumeButton);
		elements.add(optionsButton);
		// elements.add(modsButton);
		elements.add(exitButton);
	}

	@Override
	public void render(GuiDrawer drawer) {
		parentLayer.render(drawer);

		Font font = drawer.getFonts().getFont("LiberationSans-Regular", 11);
		String pauseText = gui.localization().getLocalizedString("ingame.pause");
		drawer.drawStringWithShadow(font,
				gui.getViewportWidth() / 2 - font.getWidth(pauseText) / 2,
				gui.getViewportHeight() / 2 + 48 * 3, pauseText, -1, new Vector4f(1));

		resumeButton.setPosition(gui.getViewportWidth() / 2 - resumeButton.getWidth() / 2, gui.getViewportHeight() / 2 + 24 * 2);
		optionsButton.setPosition(resumeButton.getPositionX(), gui.getViewportHeight() / 2 + 24);
		exitButton.setPosition(resumeButton.getPositionX(), gui.getViewportHeight() / 2 - 24);

		resumeButton.render(drawer);
		optionsButton.render(drawer);
		exitButton.render(drawer);
	}

	@Override
	public boolean handleInput(Input input) {
		if (input.equals("exit")) {
			gui.popTopLayer();
			return true;
		}

		super.handleInput(input);

		return true;
	}
}
