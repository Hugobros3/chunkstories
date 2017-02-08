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

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.gui.elements.Button;

public class LanguageSelectionScreen extends Overlay
{
	Button backOption = new Button(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
	List<LanguageButton> languages = new ArrayList<LanguageButton>();

	boolean allowBackButton;

	public LanguageSelectionScreen(OverlayableScene scene, Overlay parent, boolean allowBackButton)
	{
		super(scene, parent);
		// Gui buttons

		this.allowBackButton = allowBackButton;

		if (allowBackButton)
			guiHandler.add(backOption);

		for (String loc : Client.getInstance().getContent().localization().listTranslations())
		{
			LanguageButton langButton = new LanguageButton(0, 0, loc);
			// System.out.println(worldButton.toString());
			langButton.height = 64 + 8;
			guiHandler.add(langButton);
			languages.add(langButton);
		}
	}

	int scroll = 0;

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
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

			if (langButton.clicked())
			{
				Client.clientConfig.setString("language", langButton.translationCode);
				Client.getInstance().getContent().localization().loadTranslation(langButton.translationCode);
				this.mainScene.changeOverlay(this.parent);
			}
			int maxWidth = renderingContext.getWindow().getWidth() - 64 * 2;
			langButton.width = maxWidth;
			langButton.setPosition(64 + langButton.width / 2, posY);
			langButton.draw();
			posY -= 128;
		}

		if (allowBackButton)
		{
			backOption.setPosition(x + 192, 48);
			backOption.draw();
		}

		if (backOption.clicked())
		{
			this.mainScene.changeOverlay(this.parent);
		}
	}

	public class LanguageButton extends Button
	{
		int posx;
		int posy;

		String translationCode;
		String translationName;

		public int width, height;

		public LanguageButton(int x, int y, String info)
		{
			super(x, y, 0, 0, "", null, 333);
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
		public boolean isMouseOver()
		{
			return (Mouse.getX() >= posx - width / 2 - 4 && Mouse.getX() < posx + width / 2 + 4 && Mouse.getY() >= posy - height / 2 - 4 && Mouse.getY() <= posy + height / 2 + 4);
		}

		@Override
		public int draw()
		{
			width = 512;

			if (hasFocus() || isMouseOver())
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
			return width * 2 - 12;
		}

		@Override
		public void setPosition(float f, float g)
		{
			posx = (int) f;
			posy = (int) g;
		}

		@Override
		public boolean clicked()
		{
			if (clicked)
			{
				clicked = false;
				return true;
			}
			return false;
		}
	}

	@Override
	public boolean handleKeypress(int k)
	{
		return false;
	}

	@Override
	public boolean onScroll(int dx)
	{
		if (dx < 0)
			scroll++;
		else
			scroll--;
		return true;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}

}
