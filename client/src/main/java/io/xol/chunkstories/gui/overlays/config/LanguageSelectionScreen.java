//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.elements.Button;
import io.xol.chunkstories.renderer.opengl.util.CorneredBoxDrawer;
import io.xol.chunkstories.renderer.opengl.util.ObjectRenderer;

public class LanguageSelectionScreen extends Layer
{
	Button backOption = new Button(this, 0, 0, 300, "#{menu.back}");
	List<LanguageButton> languages = new ArrayList<LanguageButton>();

	boolean allowBackButton;

	public LanguageSelectionScreen(GameWindow scene, Layer parent, boolean allowBackButton)
	{
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

		for (String loc : Client.getInstance().getContent().localization().listTranslations())
		{
			LanguageButton langButton = new LanguageButton(this, 0, 0, loc);
			langButton.setAction(new Runnable() {

				@Override
				public void run() {
					//Convinience hack to set keys to wasd when first lauching and selecting English as a language
					if(!allowBackButton && langButton.translationCode.endsWith("fr"))
					{
						//azerty mode enabled
						Client.getInstance().getConfiguration().getOption("client.input.bind.forward").trySetting(""+GLFW.GLFW_KEY_Z);
						Client.getInstance().getConfiguration().getOption("client.input.bind.left").trySetting(""+GLFW.GLFW_KEY_Q);
					}
					
					Client.getInstance().getConfiguration().getOption("client.game.language").trySetting(langButton.translationCode);
					Client.getInstance().getContent().localization().loadTranslation(langButton.translationCode);
					gameWindow.setLayer(parentLayer);
				}
				
			});
			
			// System.out.println(worldButton.toString());
			langButton.height = 64 + 8;
			elements.add(langButton);
			languages.add(langButton);
		}
	}

	int scroll = 0;

	@Override
	public void render(RenderingInterface renderingContext)
	{
		if (scroll < 0)
			scroll = 0;

		this.parentLayer.getRootLayer().render(renderingContext);
		
		int posY = renderingContext.getWindow().getHeight() - 128;
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 11), 64, posY + 64, "Welcome - Bienvenue - Wilkomen - Etc", 3, 3, new Vector4f(1));
		
		int remainingSpace = (int) Math.floor(renderingContext.getWindow().getHeight() / 96 - 2);

		while (scroll + remainingSpace > languages.size())
			scroll--;

		int skip = scroll;
		for (LanguageButton langButton : languages)
		{
			if (skip-- > 0)
				continue;
			if (remainingSpace-- <= 0)
				break;

			
			int maxWidth = renderingContext.getWindow().getWidth() - 64 * 2;
			langButton.width = maxWidth;
			langButton.setPosition(64 + langButton.width / 2, posY);
			langButton.render(renderingContext);
			posY -= 128;
		}

		if (allowBackButton)
		{
			backOption.setPosition(xPosition + 192, 48);
			backOption.render(renderingContext);
		}
	}

	public class LanguageButton extends Button
	{
		int posx;
		int posy;

		String translationCode;
		String translationName;

		public int width, height;

		public LanguageButton(Layer layer, int x, int y, String info)
		{
			super(layer, x, y, 0, "");
			posx = x;
			posy = y;
			this.translationCode = info;

			try
			{
				InputStream is = Client.getInstance().getContent().getAsset("./lang/" + translationCode + "/lang.info").read();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF8"));
				
				translationName = reader.readLine();
				reader.close();
			}
			catch (IOException e)
			{

			}
		}

		@Override
		public boolean isMouseOver(Mouse mouse)
		{
			return (mouse.getCursorX() >= posx - width / 2 - 4 && mouse.getCursorX() < posx + width / 2 + 4 && mouse.getCursorY() >= posy - height / 2 - 4 && mouse.getCursorY() <= posy + height / 2 + 4);
		}

		@Override
		public void render(RenderingInterface renderer)
		{
			width = 512;

			if (isFocused() || isMouseOver())
			{
				CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, 128, 8, "./textures/gui/scalableButtonOver.png", 32, 2);
			}
			else
			{
				CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, 128, 8, "./textures/gui/scalableButton.png", 32, 2);
			}

			ObjectRenderer.renderTexturedRect(posx - width / 2 + 80, posy, 128, 96, "./lang/" + translationCode + "/lang.png");
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().getFont("LiberationSans-Regular", 11), posx - width / 2 + 150, posy, translationName, 3, 3, new Vector4f(1));
		}

		@Override
		public void setPosition(float f, float g)
		{
			posx = (int) f;
			posy = (int) g;
		}
	}

	@Override
	public boolean handleInput(Input input) {
		if(input instanceof MouseScroll) {
			MouseScroll ms = (MouseScroll)input;
			if (ms.amount() < 0)
				scroll++;
			else
				scroll--;
			return true;
		}
		
		return super.handleInput(input);
	}
}
