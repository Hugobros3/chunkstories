//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.gui.elements.LargeButtonWithIcon;
import io.xol.chunkstories.api.util.configuration.Configuration;
import io.xol.chunkstories.localization.LocalizationManagerImplementation;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.Button;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;

public class LanguageSelectionScreen extends Layer {
	private LargeButtonWithIcon backOption = new LargeButtonWithIcon(this, "back");
	private List<LanguageButton> languages = new ArrayList<LanguageButton>();

	private boolean allowBackButton;

	public LanguageSelectionScreen(Gui gui, Layer parent, boolean allowBackButton) {
		super(gui, parent);
		// Gui buttons

		this.allowBackButton = allowBackButton;

		backOption.setAction(gui::popTopLayer);

		if (allowBackButton)
			elements.add(backOption);

		for (String localization : ((LocalizationManagerImplementation)gui.localization()).listTranslations()) {
			LanguageButton langButton = new LanguageButton(this, 0, 0, localization);
			langButton.setAction(() -> {
				//Convinience hack: Have ZSQD mapped ( WASD on azerty ) when french is used as a game language
				if (!allowBackButton && langButton.translationCode.endsWith("fr")) {
					((Configuration.OptionInt)gui.getClient().getConfiguration().get("client.input.bind.forward")).trySetting(GLFW.GLFW_KEY_Z);
					((Configuration.OptionInt)gui.getClient().getConfiguration().get("client.input.bind.left")).trySetting(GLFW.GLFW_KEY_Q);
				}

				((Configuration.OptionString)gui.getClient().getConfiguration().get("client.game.language")).trySetting(langButton.translationCode);
				gui.getClient().getContent().localization().loadTranslation(langButton.translationCode);
				gui.popTopLayer();
			});

			elements.add(langButton);
			languages.add(langButton);
		}
	}

	private int scroll = 0;

	@Override
	public void render(GuiDrawer drawer) {
		if (scroll < 0)
			scroll = 0;

		this.parentLayer.render(drawer);

		int posY = (int) (gui.getViewportHeight() - (64 + 32));

		drawer.drawStringWithShadow(
				drawer.getFonts().getFont("LiberationSans-Regular", 22), 8,
				gui.getViewportHeight() - 32, "Welcome - Bienvenue - Wilkomen - Etc", -1,
				new Vector4f(1));

		int remainingSpace = gui.getViewportHeight() / 96 - 2;

		while (scroll + remainingSpace > languages.size())
			scroll--;

		int skip = scroll;
		for (LanguageButton langButton : languages) {
			if (skip-- > 0)
				continue;
			if (remainingSpace-- <= 0)
				break;

			langButton.setWidth(256);
			langButton.setPosition(gui.getViewportWidth() / 2 - langButton.getWidth() / 2, posY);
			langButton.render(drawer);
			posY -= langButton.getHeight() + (4);
		}

		if (allowBackButton) {
			backOption.setPosition(8, 8);
			backOption.render(drawer);
		}
	}

	public class LanguageButton extends Button {
		String translationCode;
		String translationName;

		LanguageButton(Layer layer, int x, int y, String info) {
			super(layer, x, y, 0, "");
			this.translationCode = info;

			try {
				InputStream is = gui.getClient().getContent().getAsset("./lang/" + translationCode + "/lang.info").read();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

				translationName = reader.readLine();
				reader.close();
			} catch (IOException e) {

			}
		}

		@Override
		public void render(GuiDrawer drawer) {
			this.setHeight(64);

			String texture = ((isFocused() || isMouseOver()) ? "./textures/gui/scalableButtonOver.png"
							: "./textures/gui/scalableButton.png");

			drawer.drawBoxWithCorners(xPosition, yPosition, getWidth(), getHeight(), 8, texture);
			//TODO ObjectRenderer.renderTexturedRect(xPosition + 40 * 1, yPosition + 32 * 1, 64 * 1, 48 * 1, "./lang/" + translationCode + "/lang.png");
			drawer.drawStringWithShadow(drawer.getFonts().getFont("LiberationSans-Regular", 11),
					xPosition + 64 + 16, yPosition + 32, translationName, -1, new Vector4f(1));
		}
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
}
