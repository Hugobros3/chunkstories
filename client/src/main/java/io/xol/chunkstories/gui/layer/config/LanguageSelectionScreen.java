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
import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.gui.elements.LargeButtonIcon;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;

public class LanguageSelectionScreen extends Layer {
	private LargeButtonIcon backOption = new LargeButtonIcon(this, "back");
	private List<LanguageButton> languages = new ArrayList<LanguageButton>();

	private boolean allowBackButton;

	public LanguageSelectionScreen(Gui gui, Layer parent, boolean allowBackButton) {
		super(gui, parent);
		// Gui buttons

		this.allowBackButton = allowBackButton;

		backOption.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}
		});

		if (allowBackButton)
			elements.add(backOption);

		for (String loc : ClientImplementation.getInstance().getContent().localization().listTranslations()) {
			LanguageButton langButton = new LanguageButton(this, 0, 0, loc);
			langButton.setAction(new Runnable() {

				@Override
				public void run() {
					// Convinience hack to set keys to wasd when first lauching and selecting
					// English as a language
					if (!allowBackButton && langButton.translationCode.endsWith("fr")) {
						// azerty mode enabled
						ClientImplementation.getInstance().getConfiguration().getOption("client.input.bind.forward")
								.trySetting("" + GLFW.GLFW_KEY_Z);
						ClientImplementation.getInstance().getConfiguration().getOption("client.input.bind.left")
								.trySetting("" + GLFW.GLFW_KEY_Q);
					}

					ClientImplementation.getInstance().getConfiguration().getOption("client.game.language")
							.trySetting(langButton.translationCode);
					ClientImplementation.getInstance().getContent().localization().loadTranslation(langButton.translationCode);
					gameWindow.setLayer(parentLayer);
				}

			});

			elements.add(langButton);
			languages.add(langButton);
		}
	}

	private int scroll = 0;

	@Override
	public void render(GuiDrawer drawer) {
		int scale = 1;
		if (scroll < 0)
			scroll = 0;

		this.parentLayer.getRootLayer().render(drawer);

		int posY = (int) (drawer.getWindow().getHeight() - scale * (64 + 32));

		drawer.getFontRenderer().drawStringWithShadow(
				drawer.getFontRenderer().getFont("LiberationSans-Regular", 11 * scale), 8 * scale,
				drawer.getWindow().getHeight() - 32 * scale, "Welcome - Bienvenue - Wilkomen - Etc", 2, 2,
				new Vector4f(1));

		int remainingSpace = (int) Math.floor(drawer.getWindow().getHeight() / 96 - 2);

		while (scroll + remainingSpace > languages.size())
			scroll--;

		int skip = scroll;
		for (LanguageButton langButton : languages) {
			if (skip-- > 0)
				continue;
			if (remainingSpace-- <= 0)
				break;

			// int maxWidth = renderingContext.getWindow().getWidth() - 64 * 2;
			langButton.setWidth(256);// maxWidth / scale);
			langButton.setPosition(drawer.getWindow().getWidth() / 2 - langButton.getWidth() / 2, posY);
			langButton.render(drawer);
			posY -= langButton.getHeight() + (4) * scale;
		}

		if (allowBackButton) {
			backOption.setPosition(8, 8);
			backOption.render(drawer);
		}
	}

	public class LanguageButton extends BaseButton {
		String translationCode;
		String translationName;

		LanguageButton(Layer layer, int x, int y, String info) {
			super(layer, x, y, 0, "");
			this.translationCode = info;

			this.height = 32;

			try {
				InputStream is = ClientImplementation.getInstance().getContent().getAsset("./lang/" + translationCode + "/lang.worldInfo")
						.read();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF8"));

				translationName = reader.readLine();
				reader.close();
			} catch (IOException e) {

			}
		}

		@Override
		public void render(RenderingInterface renderer) {
			// width = 256;
			this.height = 64;

			Texture2D texture = renderer.textures()
					.getTexture((isFocused() || isMouseOver()) ? "./textures/gui/scalableButtonOver.png"
							: "./textures/gui/scalableButton.png");
			texture.setLinearFiltering(false);

			renderer.getGuiRenderer().drawCorneredBoxTiled(xPosition, yPosition, getWidth(), getHeight(), 4, texture,
					32, scale());

			ObjectRenderer.renderTexturedRect(xPosition + 40 * scale(), yPosition + 32 * scale(), 64 * scale(),
					48 * scale(), "./lang/" + translationCode + "/lang.png");

			renderer.getFontRenderer().drawStringWithShadow(
					renderer.getFontRenderer().getFont("LiberationSans-Regular", 11 * scale()),
					xPosition + 64 * scale() + 16 * scale(), yPosition + 32 * scale(), translationName, 2, 2,
					new Vector4f(1));
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
