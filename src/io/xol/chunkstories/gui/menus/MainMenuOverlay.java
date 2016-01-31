package io.xol.chunkstories.gui.menus;

import java.util.Random;

import org.lwjgl.input.Keyboard;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.entity.inventory.Inventory;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
//import io.xol.engine.base.font.TrueTypeFont;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;

public class MainMenuOverlay extends MenuOverlay
{

	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();
	ClickableButton singlePlayer = new ClickableButton(0, 0, 300, 32, ("Single player [demo]"), BitmapFont.SMALLFONTS, 1);
	ClickableButton multiPlayer = new ClickableButton(0, 0, 300, 32, ("Find a server ... "), BitmapFont.SMALLFONTS, 1);
	ClickableButton optionsMenu = new ClickableButton(0, 0, 300, 32, ("Game options"), BitmapFont.SMALLFONTS, 1);
	ClickableButton exitGame = new ClickableButton(0, 0, 300, 32, ("Exit game"), BitmapFont.SMALLFONTS, 1);

	public MainMenuOverlay(OverlayableScene scene, MenuOverlay parent)
	{
		super(scene, parent);
		// Gui buttons
		guiHandler.add(singlePlayer);
		guiHandler.add(multiPlayer);
		guiHandler.add(optionsMenu);
		guiHandler.add(exitGame);
	}

	public void drawToScreen(int x, int y, int w, int h)
	{
		ObjectRenderer.renderTexturedRectAlpha(384 - 32 - 4, XolioWindow.frameH - 192, 768, 768, "logo", 1f);
		FontRenderer2.drawTextUsingSpecificFontRVBA(384 + 192, XolioWindow.frameH - 256 - 16, 0, 48, "Indev " + VersionInfo.version, BitmapFont.SMALLFONTS, 1, 0.5f, 1, 1);

		// ObjectRenderer.renderTexturedRectAlpha(XolioWindow.frameW/2,
		// XolioWindow.frameH / 2 + 256, 1024, 1024, "logo", 1f);
		// FontRenderer2.drawTextUsingSpecificFontRVBA(XolioWindow.frameW/2+192,
		// XolioWindow.frameH / 2 + 160, 0, 48, "Indev " + VersionInfo.version,
		// BitmapFont.SMALLFONTS, 1, 0.5f, 1, 1);

		Random rng = new Random();
		rng.setSeed(System.currentTimeMillis() / 100);

		char[] bytes = new char[16];
		for (int i = 0; i < 16; i++)
			bytes[i] = (char) ((rng.nextInt() % 512));

		// String random = new String(bytes);

		//FontRenderer2.drawTextUsingSpecificFontRVBA(500, 500, Math.sin(a) * 15f, 32, "ntm", , alpha, r, v, b)

		//TrueTypeFont.haettenschweiler.drawString(384 + 192, XolioWindow.frameH - 256 - 16, "Сука Блять ", 2f, 2f);
		// TrueTypeFont.arial12.drawStringWithShadow(384 + 192,
		// XolioWindow.frameH - 256 - 16, "Cyka blyat Сука Блять", 2f, 2f, new
		// Vector4f(0,1,1,1));
		// FontRenderer2.drawTextUsingSpecificFontRVBA(384 + 192,
		// XolioWindow.frameH - 256 - 48, 0, 32, "Cyka blyat",
		// BitmapFont.SMALLFONTS, 1, 1f, 1, 1);

		singlePlayer.setPos(x + 220, XolioWindow.frameH - 320);
		singlePlayer.draw();

		multiPlayer.setPos(x + 220, XolioWindow.frameH - 320 - 48);
		multiPlayer.draw();

		optionsMenu.setPos(x + 220, XolioWindow.frameH - 320 - 48 * 2);
		optionsMenu.draw();

		exitGame.setPos(x + 220, XolioWindow.frameH - 320 - 48 * 3);
		exitGame.draw();

		if (singlePlayer.clicked())
		{
			mainScene.changeOverlay(new LevelSelectOverlay(mainScene, this));
		}
		if (multiPlayer.clicked())
		{
			mainScene.changeOverlay(new ServerSelectionOverlay(mainScene, this, false));
			// mainScene.eng.changeScene(new ServerSelectionScene(mainScene.eng,
			// false));
		}
		if (optionsMenu.clicked())
		{
			mainScene.changeOverlay(new OptionsOverlay(mainScene, this));
		}
		if (exitGame.clicked())
		{
			this.mainScene.eng.close();
		}

		String version = "ChunkStories " + VersionInfo.version + " - (c) 2015 XolioWare Interactive";
		FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW - 20 - FontRenderer2.getTextLengthUsingFont(32, version, BitmapFont.SMALLFONTS), 10, 0, 32, version, BitmapFont.SMALLFONTS);
	}

	public boolean handleKeypress(int k)
	{
		if (k == Keyboard.KEY_E)
			mainScene.changeOverlay(new InventoryOverlay(mainScene, this, new Inventory(10, 4, "Inventaire")));

		if (k == Keyboard.KEY_R)
			mainScene.changeOverlay(new MessageBoxOverlay(mainScene, this, "Error : error"));
		return false;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
