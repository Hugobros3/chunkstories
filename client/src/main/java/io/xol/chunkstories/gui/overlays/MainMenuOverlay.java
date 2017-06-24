package io.xol.chunkstories.gui.overlays;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.ng.NgButton;
import io.xol.chunkstories.gui.overlays.config.LogPolicyAsk;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MainMenuOverlay extends Layer
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	NgButton singlePlayer = new NgButton(this, 0, 0,("#{menu.singleplayer}"));
	NgButton multiPlayer = new NgButton(this, 0, 0, ("#{menu.serverbrowser}"));
	NgButton modsOption = new NgButton(this, 0, 0,("#{menu.mods}"));
	NgButton optionsMenu = new NgButton(this, 0, 0,("#{menu.options}"));
	NgButton exitGame = new NgButton(this, 0, 0, ("#{menu.quit}"));
	
	//NgButton k = new NgButton(this, 0, 0, "Singleplayer");

	public MainMenuOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		// Gui buttons
		this.singlePlayer.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new LevelSelectOverlay(gameWindow, MainMenuOverlay.this));
			}
		});
		
		this.multiPlayer.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new ServerSelectionOverlayNg(gameWindow, MainMenuOverlay.this, false));
			}
		});
		
		this.modsOption.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new ModsSelectionOverlay(gameWindow, MainMenuOverlay.this));
			}
		});
		
		this.optionsMenu.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new OptionsOverlay(gameWindow, MainMenuOverlay.this));
			}
		});
		
		this.exitGame.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.close();
			}
		});
		
		elements.add(singlePlayer);
		elements.add(multiPlayer);
		elements.add(modsOption);
		elements.add(optionsMenu);
		elements.add(exitGame);
		
		//elements.add(k);
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.render(renderingContext);
		
		if(Client.clientConfig.getProp("log-policy", "undefined").equals("undefined"))
			gameWindow.setLayer(new LogPolicyAsk(gameWindow, this));
			//this.mainScene.changeOverlay(new LogPolicyAsk(mainScene, this));
		
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
		singlePlayer.render(renderingContext);

		multiPlayer.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += multiPlayer.getWidth() + spacing;
		multiPlayer.render(renderingContext);

		modsOption.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += modsOption.getWidth() + spacing;
		modsOption.render(renderingContext);

		optionsMenu.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += optionsMenu.getWidth() + spacing;
		optionsMenu.render(renderingContext);

		exitGame.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += exitGame.getWidth() + spacing;
		exitGame.render(renderingContext);

		Vector4fm noticeColor = new Vector4fm(100/255f, 100/255f, 100/255f, 1);
		String version = "Chunk Stories Client " + VersionInfo.version;
		renderingContext.getFontRenderer().defaultFont().getWidth(version);
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), 4, 0, version, 1, noticeColor);
	
		String copyrightNotice = "Copyright (c) 2016-2017 XolioWare Interactive";
		float noticeDekal = renderingContext.getFontRenderer().defaultFont().getWidth(copyrightNotice);
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), renderingContext.getWindow().getWidth() - noticeDekal - 4, 0, copyrightNotice, 1, noticeColor);
	
	}

	/*@Override
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
	}*/
}
