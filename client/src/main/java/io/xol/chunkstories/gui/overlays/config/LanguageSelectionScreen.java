package io.xol.chunkstories.gui.overlays.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.gui.elements.Button;

public class LanguageSelectionScreen extends Layer
{
	Button backOption = new Button(this, 0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
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
					if(!allowBackButton && langButton.translationCode.endsWith("en"))
					{
						//Englishfag detected, thanks /u/MrSmith33 for feedback
						Client.clientConfig.setInteger("bind.forward", GLFW.GLFW_KEY_W);
						Client.clientConfig.setInteger("bind.left", GLFW.GLFW_KEY_A);
					}
					
					Client.clientConfig.setString("language", langButton.translationCode);
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

		int posY = renderingContext.getWindow().getHeight() - 128;
		FontRenderer2.drawTextUsingSpecificFont(64, posY + 64, 0, 48, "Welcome - Bienvenue - Wilkomen - Etc", BitmapFont.SMALLFONTS);
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
			super(layer, x, y, 0, 0, "", null, 333);
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

			//System.out.println(GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");
			ObjectRenderer.renderTexturedRect(posx - width / 2 + 80, posy, 128, 96, "./lang/" + translationCode + "/lang.png");

			//System.out.println("a+"+GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");

			FontRenderer2.setLengthCutoff(true, width - 72);
			FontRenderer2.drawTextUsingSpecificFont(posx - width / 2 + 150, posy, 0, 2 * 32, translationName, BitmapFont.SMALLFONTS);
			//FontRenderer2.drawTextUsingSpecificFontRVBA(posx - width / 2 + 72, posy - 32, 0, 1 * 32, info.getDescription(), BitmapFont.SMALLFONTS, 1.0f, 0.8f, 0.8f, 0.8f);
			FontRenderer2.setLengthCutoff(false, -1);
			
			//return width * 2 - 12;
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

	/*@Override
	public boolean onScroll(int dx)
	{
		if (dx < 0)
			scroll++;
		else
			scroll--;
		return true;
	}*/

	
	
	/*@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}*/
}
