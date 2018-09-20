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

import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.gui.elements.LargeButtonIcon;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.renderer.opengl.util.ObjectRenderer;

public class LanguageSelectionScreen extends Layer {
	LargeButtonIcon backOption = new LargeButtonIcon(this, "back");
	List<LanguageButton> languages = new ArrayList<LanguageButton>();

	boolean allowBackButton;

	public LanguageSelectionScreen(GameWindow scene, Layer parent, boolean allowBackButton) {
		super(scene, parent);
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

	int scroll = 0;

	@Override
	public void render(RenderingInterface renderingContext) {
		float scale = this.getGuiScale();
		if (scroll < 0)
			scroll = 0;

		this.parentLayer.getRootLayer().render(renderingContext);

		int posY = (int) (renderingContext.getWindow().getHeight() - scale * (64 + 32));

		renderingContext.getFontRenderer().drawStringWithShadow(
				renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 11 * scale), 8 * scale,
				renderingContext.getWindow().getHeight() - 32 * scale, "Welcome - Bienvenue - Wilkomen - Etc", 2, 2,
				new Vector4f(1));

		int remainingSpace = (int) Math.floor(renderingContext.getWindow().getHeight() / 96 - 2);

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
			langButton.setPosition(renderingContext.getWindow().getWidth() / 2 - langButton.getWidth() / 2, posY);
			langButton.render(renderingContext);
			posY -= langButton.getHeight() + (4) * scale;
		}

		if (allowBackButton) {
			backOption.setPosition(8, 8);
			backOption.render(renderingContext);
		}
	}

	public class LanguageButton extends BaseButton {
		String translationCode;
		String translationName;

		public LanguageButton(Layer layer, int x, int y, String info) {
			super(layer, x, y, 0, "");
			this.translationCode = info;

			this.height = 32;

			try {
				InputStream is = ClientImplementation.getInstance().getContent().getAsset("./lang/" + translationCode + "/lang.info")
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
