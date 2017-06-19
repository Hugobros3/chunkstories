package io.xol.chunkstories.gui.overlays;

import org.lwjgl.input.Keyboard;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.ng.NgButton;
import io.xol.chunkstories.gui.overlays.config.LogPolicyAsk;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay;
import io.xol.chunkstories.gui.overlays.general.MessageBoxOverlay;
import io.xol.chunkstories.gui.overlays.ingame.DeathOverlay;
import io.xol.chunkstories.gui.overlays.ingame.InventoryOverlay;
import io.xol.chunkstories.item.inventory.BasicInventory;
import io.xol.chunkstories.item.inventory.InventoryLocalCreativeMenu;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.gui.Overlay;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MainMenuOverlay extends Overlay
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	NgButton singlePlayer = new NgButton(0, 0,("#{menu.singleplayer}"));
	NgButton multiPlayer = new NgButton(0, 0, ("#{menu.serverbrowser}"));
	NgButton modsOption = new NgButton(0, 0,("#{menu.mods}"));
	NgButton optionsMenu = new NgButton(0, 0,("#{menu.options}"));
	NgButton exitGame = new NgButton(0, 0, ("#{menu.quit}"));
	
	NgButton k = new NgButton(0, 0, "Singleplayer");

	public MainMenuOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		// Gui buttons
		guiHandler.add(singlePlayer);
		guiHandler.add(multiPlayer);
		guiHandler.add(modsOption);
		guiHandler.add(optionsMenu);
		guiHandler.add(exitGame);
		
		guiHandler.add(k);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		if(Client.clientConfig.getProp("log-policy", "undefined").equals("undefined"))
		{
			this.mainScene.changeOverlay(new LogPolicyAsk(mainScene, this));
		}
		
		float totalLengthOfButtons = 0;
		float spacing = -1;
		
		totalLengthOfButtons += singlePlayer.getWidth();
		totalLengthOfButtons += spacing;
		
		totalLengthOfButtons += multiPlayer.getWidth();
		totalLengthOfButtons += spacing;
		
		totalLengthOfButtons += modsOption.getWidth();
		totalLengthOfButtons += spacing;
		
		totalLengthOfButtons += optionsMenu.getWidth();
		totalLengthOfButtons += spacing;
		
		totalLengthOfButtons += exitGame.getWidth();
		
		float buttonDisplayX = renderingContext.getWindow().getWidth() / 2 - totalLengthOfButtons / 2;
		float buttonDisplayY = 32;
		
		singlePlayer.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += singlePlayer.getWidth() + spacing;
		singlePlayer.draw();

		multiPlayer.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += multiPlayer.getWidth() + spacing;
		multiPlayer.draw();

		modsOption.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += modsOption.getWidth() + spacing;
		modsOption.draw();

		optionsMenu.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += optionsMenu.getWidth() + spacing;
		optionsMenu.draw();

		exitGame.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += exitGame.getWidth() + spacing;
		exitGame.draw();

		if (singlePlayer.clicked())
			mainScene.changeOverlay(new LevelSelectOverlay(mainScene, this));
		else if (multiPlayer.clicked())
			mainScene.changeOverlay(new ServerSelectionOverlayNg(mainScene, this, false));
		else if (modsOption.clicked())
			mainScene.changeOverlay(new ModsSelectionOverlay(mainScene, this));
		else if (optionsMenu.clicked())
			mainScene.changeOverlay(new OptionsOverlay(mainScene, this));
		else if (exitGame.clicked())
			this.mainScene.gameWindow.close();

		Vector4fm noticeColor = new Vector4fm(100/255f, 100/255f, 100/255f, 1);
		String version = "Chunk Stories Client " + VersionInfo.version;
		renderingContext.getFontRenderer().defaultFont().getWidth(version);
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), 4, 0, version, 1, noticeColor);
	
		String copyrightNotice = "Copyright (c) 2016-2017 XolioWare Interactive";
		float noticeDekal = renderingContext.getFontRenderer().defaultFont().getWidth(copyrightNotice);
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), renderingContext.getWindow().getWidth() - noticeDekal - 4, 0, copyrightNotice, 1, noticeColor);
	
	}

	@Override
	public boolean handleKeypress(int k)
	{
		if (k == Keyboard.KEY_E)
			mainScene.changeOverlay(new InventoryOverlay(mainScene, this, new Inventory[]{new BasicInventory(10, 4)
			, new InventoryLocalCreativeMenu()})); // new Inventory(null, 10, 4, "La chatte à ta mère")
		if (k == Keyboard.KEY_D)
			mainScene.changeOverlay(new DeathOverlay(mainScene, this));

		if (k == Keyboard.KEY_R)
			mainScene.changeOverlay(new MessageBoxOverlay(mainScene, this, "Error : error"));
		
		if (k == Keyboard.KEY_C && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
		{
			//Fabricated crash
			throw new RuntimeException("Epic crash");
		}
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
