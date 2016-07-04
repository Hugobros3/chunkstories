package io.xol.chunkstories.gui.menus;

import java.util.Random;

import org.lwjgl.input.Keyboard;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.core.entity.components.EntityComponentInventory;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.item.inventory.InventoryAllVoxels;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
//import io.xol.engine.base.font.TrueTypeFont;
import io.xol.engine.gui.GuiElementsHandler;
import io.xol.engine.gui.elements.Button;

public class MainMenuOverlay extends Overlay
{
	GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button singlePlayer = new Button(0, 0, 300, 32, ("Single player"), BitmapFont.SMALLFONTS, 1);
	Button multiPlayer = new Button(0, 0, 300, 32, ("Find a server ... "), BitmapFont.SMALLFONTS, 1);
	Button optionsMenu = new Button(0, 0, 300, 32, ("Game options"), BitmapFont.SMALLFONTS, 1);
	Button exitGame = new Button(0, 0, 300, 32, ("Exit game"), BitmapFont.SMALLFONTS, 1);

	public MainMenuOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		// Gui buttons
		guiHandler.add(singlePlayer);
		guiHandler.add(multiPlayer);
		guiHandler.add(optionsMenu);
		guiHandler.add(exitGame);
	}

	@Override
	public void drawToScreen(int x, int y, int w, int h)
	{
		if(Client.clientConfig.getProp("log-policy", "undefined").equals("undefined"))
		{
			this.mainScene.changeOverlay(new LogPolicyAsk(mainScene, this));
		}
		
		ObjectRenderer.renderTexturedRectAlpha(384 - 32 - 4, GameWindowOpenGL.windowHeight - 192, 768, 768, "logo", 1f);
		FontRenderer2.drawTextUsingSpecificFontRVBA(384 + 192, GameWindowOpenGL.windowHeight - 256 - 16, 0, 48, "Indev " + VersionInfo.version, BitmapFont.SMALLFONTS, 1, 0.5f, 1, 1);

		Random rng = new Random();
		rng.setSeed(System.currentTimeMillis() / 100);

		char[] bytes = new char[16];
		for (int i = 0; i < 16; i++)
			bytes[i] = (char) ((rng.nextInt() % 512));

		singlePlayer.setPosition(x + 220, GameWindowOpenGL.windowHeight - 320);
		singlePlayer.draw();

		multiPlayer.setPosition(x + 220, GameWindowOpenGL.windowHeight - 320 - 48);
		multiPlayer.draw();

		optionsMenu.setPosition(x + 220, GameWindowOpenGL.windowHeight - 320 - 48 * 2);
		optionsMenu.draw();

		exitGame.setPosition(x + 220, GameWindowOpenGL.windowHeight - 320 - 48 * 3);
		exitGame.draw();

		if (singlePlayer.clicked())
			mainScene.changeOverlay(new LevelSelectOverlay(mainScene, this));
		else if (multiPlayer.clicked())
			mainScene.changeOverlay(new ServerSelectionOverlay(mainScene, this, false));
		else if (optionsMenu.clicked())
			mainScene.changeOverlay(new OptionsOverlay(mainScene, this));
		else if (exitGame.clicked())
			this.mainScene.eng.close();

		String version = "ChunkStories " + VersionInfo.version + " - (c) 2016 XolioWare Interactive";
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth - 20 - FontRenderer2.getTextLengthUsingFont(32, version, BitmapFont.SMALLFONTS), 10, 0, 32, version, BitmapFont.SMALLFONTS);
	}

	@Override
	public boolean handleKeypress(int k)
	{
		if (k == Keyboard.KEY_E)
			mainScene.changeOverlay(new InventoryOverlay(mainScene, this, new EntityComponentInventory[]{new EntityComponentInventory(null, 10, 4)
			, new InventoryAllVoxels()})); // new Inventory(null, 10, 4, "La chatte à ta mère")

		if (k == Keyboard.KEY_R)
			mainScene.changeOverlay(new MessageBoxOverlay(mainScene, this, "Error : error"));
		return false;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
