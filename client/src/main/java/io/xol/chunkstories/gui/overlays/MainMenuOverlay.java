package io.xol.chunkstories.gui.overlays;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.gui.Layer;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.ng.LargeButton;
import io.xol.chunkstories.gui.ng.LargeButtonIcon;
import io.xol.chunkstories.gui.overlays.config.LogPolicyAsk;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MainMenuOverlay extends Layer
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	/*ThinButton singlePlayer = new ThinButton(this, 0, 0,("#{menu.singleplayer}"));
	ThinButton multiPlayer = new ThinButton(this, 0, 0, ("#{menu.serverbrowser}"));
	ThinButton modsOption = new ThinButton(this, 0, 0,("#{menu.mods}"));
	ThinButton optionsMenu = new ThinButton(this, 0, 0,("#{menu.options}"));
	ThinButton exitGame = new ThinButton(this, 0, 0, ("#{menu.quit}"));*/
	
	LargeButtonIcon largeOnline = new LargeButtonIcon(this, "online");
	LargeButtonIcon largeMods = new LargeButtonIcon(this, "mods");
	
	LargeButton largeSingleplayer = new LargeButton(this, "singleplayer");
	LargeButton largeOptions = new LargeButton(this, "options");
	
	//NgButton k = new NgButton(this, 0, 0, "Singleplayer");

	public MainMenuOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		// Gui buttons
		this.largeSingleplayer.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new LevelSelectOverlay(gameWindow, MainMenuOverlay.this));
			}
		});
		
		this.largeOnline.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new ServerSelectionOverlayNg(gameWindow, MainMenuOverlay.this, false));
			}
		});
		
		this.largeMods.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new ModsSelectionOverlay(gameWindow, MainMenuOverlay.this));
			}
		});
		
		this.largeOptions.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new OptionsOverlay(gameWindow, MainMenuOverlay.this));
			}
		});
		
		/*this.exitGame.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.close();
			}
		});
		
		elements.add(singlePlayer);
		elements.add(multiPlayer);
		elements.add(modsOption);
		elements.add(optionsMenu);
		elements.add(exitGame);*/
		
		elements.add(largeOnline);
		elements.add(largeMods);
		
		elements.add(largeSingleplayer);
		elements.add(largeOptions);
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.render(renderingContext);
		
		if(gameWindow.getLayer() == this && Client.getInstance().configDeprecated().getString("log-policy", "undefined").equals("undefined"))
			gameWindow.setLayer(new LogPolicyAsk(gameWindow, this));

		float spacing = 8;
		float buttonsAreaSize = (96 * 2 + spacing) * this.getGuiScale();
		
		float leftButtonX = this.getWidth() / 2 - buttonsAreaSize / 2;
		
		float ySmall = (12) * this.getGuiScale();
		float yBig = ySmall + (24 + spacing) * this.getGuiScale();
		
		largeOnline.setPosition(leftButtonX, yBig);
		largeOnline.render(renderingContext);

		largeSingleplayer.setPosition(leftButtonX, ySmall);
		largeSingleplayer.render(renderingContext);
		
		float rightButtonX = leftButtonX + (spacing + 96) * this.getGuiScale();
		
		largeMods.setPosition(rightButtonX, yBig);
		largeMods.render(renderingContext);

		largeOptions.setPosition(rightButtonX, ySmall);
		largeOptions.render(renderingContext);
		
		//Notices
		Vector4f noticeColor = new Vector4f(0.5f);
		String version = "Chunk Stories Client " + VersionInfo.version;
		renderingContext.getFontRenderer().defaultFont().getWidth(version);
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), 4, 0, version, this.getGuiScale(), noticeColor);
	
		String copyrightNotice = "Copyright (c) 2016-2017 XolioWare Interactive";
		float noticeDekal = renderingContext.getFontRenderer().defaultFont().getWidth(copyrightNotice) * (this.getGuiScale());
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), renderingContext.getWindow().getWidth() - noticeDekal - 4, 0, copyrightNotice, this.getGuiScale(), noticeColor);
	
	}

	//TODO re-include some of that stuff
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
	}*/
}
