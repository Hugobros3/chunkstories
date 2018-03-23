//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.gui.layer.config.ModsSelection;
import io.xol.chunkstories.gui.layer.config.OptionsScreen;

/** The GUI code for the basic pause menu you bring about by pressing ESC */
public class PauseMenu extends Layer {
	BaseButton resumeButton = new BaseButton(this, 0, 0, 160, "#{menu.resume}");
	BaseButton optionsButton = new BaseButton(this, 0, 0, 160, "#{menu.options}");
	BaseButton modsButton = new BaseButton(this, -100, 0, 160, "#{menu.mods}");
	BaseButton exitButton = new BaseButton(this, 0, 0, 160, "#{menu.backto}");

	public PauseMenu(GameWindow scene, Layer parent) {
		super(scene, parent);

		this.resumeButton.setAction(() -> gameWindow.setLayer(parentLayer));

		this.optionsButton.setAction(() -> gameWindow.setLayer(new OptionsScreen(gameWindow, PauseMenu.this)));

		this.modsButton.setAction(() -> gameWindow.setLayer(new ModsSelection(gameWindow, PauseMenu.this)));

		this.exitButton.setAction(() -> gameWindow.getClient().exitToMainMenu());

		elements.add(resumeButton);
		elements.add(optionsButton);
		// elements.add(modsButton);
		elements.add(exitButton);
	}

	@Override
	public void render(RenderingInterface renderer) {
		parentLayer.render(renderer);

		Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
		String pauseText = renderer.getClient().getContent().localization().getLocalizedString("ingame.pause");
		renderer.getFontRenderer().drawStringWithShadow(font,
				renderer.getWindow().getWidth() / 2 - font.getWidth(pauseText) * 1.5f,
				renderer.getWindow().getHeight() / 2 + 48 * 3, pauseText, 3, 3, new Vector4f(1));

		resumeButton.setPosition(renderer.getWindow().getWidth() / 2 - resumeButton.getWidth() / 2,
				renderer.getWindow().getHeight() / 2 + 24 * 2 * getGuiScale());
		optionsButton.setPosition(resumeButton.getPositionX(),
				renderer.getWindow().getHeight() / 2 + 24 * getGuiScale());
		exitButton.setPosition(resumeButton.getPositionX(), renderer.getWindow().getHeight() / 2 - 24 * getGuiScale());

		resumeButton.render(renderer);
		optionsButton.render(renderer);
		exitButton.render(renderer);
	}

	@Override
	public boolean handleInput(Input input) {
		if (input.equals("exit")) {
			gameWindow.setLayer(parentLayer);
			return true;
		}

		super.handleInput(input);

		return true;
	}
}
