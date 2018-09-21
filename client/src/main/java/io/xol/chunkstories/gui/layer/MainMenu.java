//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.LargeButton;
import io.xol.chunkstories.api.gui.elements.LargeButtonIcon;
import io.xol.chunkstories.api.item.inventory.BasicInventory;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.gui.layer.config.LanguageSelectionScreen;
import io.xol.chunkstories.gui.layer.config.LogPolicyAsk;
import io.xol.chunkstories.gui.layer.config.ModsSelection;
import io.xol.chunkstories.gui.layer.config.OptionsScreen;
import io.xol.chunkstories.gui.layer.ingame.DeathScreen;
import io.xol.chunkstories.gui.layer.ingame.InventoryView;
import io.xol.chunkstories.util.VersionInfo;

/** Gives quick access to the main features of the game */
public class MainMenu extends Layer {
	LargeButtonIcon largeOnline = new LargeButtonIcon(this, "online");
	LargeButtonIcon largeMods = new LargeButtonIcon(this, "mods");

	LargeButton largeSingleplayer = new LargeButton(this, "singleplayer");
	LargeButton largeOptions = new LargeButton(this, "options");

	public MainMenu(Gui scene, Layer parent) {
		super(scene, parent);

		this.largeSingleplayer.setAction(() -> gameWindow.setLayer(new LevelSelection(gameWindow, MainMenu.this)));
		this.largeOnline.setAction(() -> gameWindow.setLayer(new ServerSelection(gameWindow, MainMenu.this, false)));
		this.largeMods.setAction(() -> gameWindow.setLayer(new ModsSelection(gameWindow, MainMenu.this)));
		this.largeOptions.setAction(() -> gameWindow.setLayer(new OptionsScreen(gameWindow, MainMenu.this)));

		largeOnline.setWidth(104);
		largeSingleplayer.setWidth(104);
		largeMods.setWidth(104);
		largeOptions.setWidth(104);

		elements.add(largeOnline);
		elements.add(largeMods);

		elements.add(largeSingleplayer);
		elements.add(largeOptions);
	}

	@Override
	public void render(GuiDrawer renderingContext) {
		parentLayer.render(renderingContext);

		if (gameWindow.getLayer() == this && gameWindow.getClient().getConfiguration()
				.getStringOption("client.game.log-policy").equals("undefined"))
			gameWindow.setLayer(new LogPolicyAsk(gameWindow, this));

		float spacing = 4;
		float buttonsAreaSize = largeSingleplayer.getWidth() * 2 + spacing * this.getGuiScale();

		float leftButtonX = this.getWidth() / 2 - buttonsAreaSize / 2 + 0.0f;

		float ySmall = (12) * this.getGuiScale();
		float yBig = ySmall + largeSingleplayer.getHeight() + (spacing) * this.getGuiScale();

		largeOnline.setPosition(leftButtonX, yBig);
		largeOnline.render(renderingContext);

		largeSingleplayer.setPosition(leftButtonX, ySmall);
		largeSingleplayer.render(renderingContext);

		float rightButtonX = leftButtonX + largeSingleplayer.getWidth() + (spacing) * this.getGuiScale();

		largeMods.setPosition(rightButtonX, yBig);
		largeMods.render(renderingContext);

		largeOptions.setPosition(rightButtonX, ySmall);
		largeOptions.render(renderingContext);

		// Notices
		Vector4f noticeColor = new Vector4f(0.5f);
		String version = "Chunk Stories ClientImplementation " + VersionInfo.version;
		renderingContext.getFontRenderer().defaultFont().getWidth(version);
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), 4, 0, version,
				this.getGuiScale(), noticeColor);

		String copyrightNotice = "2015-2018 Hugo 'Gobrosse' Devillers";
		float noticeDekal = renderingContext.getFontRenderer().defaultFont().getWidth(copyrightNotice)
				* (this.getGuiScale());
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(),
				renderingContext.getWindow().getWidth() - noticeDekal - 4, 0, copyrightNotice, this.getGuiScale(),
				noticeColor);

	}

	@Override
	public boolean handleTextInput(char c) {
		if (c == 'e') {
			gameWindow.setLayer(new InventoryView(gameWindow, this, new Inventory[] { new BasicInventory(10, 4) }));
		} else if (c == 'd') {
			gameWindow.setLayer(new DeathScreen(gameWindow, this));
		} else if (c == 'r') {
			gameWindow.setLayer(new MessageBox(gameWindow, this, "Error : error"));
		} else if (c == 'l') {
			gameWindow.setLayer(new LanguageSelectionScreen(gameWindow, this, true));
		} else if (c == 'o') {
			gameWindow.setLayer(new LogPolicyAsk(gameWindow, this));
		} else if (c == 'c') {
			// Fabricated crash
			throw new RuntimeException("Epic crash");
		}

		return super.handleTextInput(c);
	}

	// TODO re-include some of that stuff
	/*
	 * @Override public boolean handleKeypress(int k) { if (k == Keyboard.KEY_E)
	 * mainScene.changeOverlay(new InventoryOverlay(mainScene, this, new
	 * Inventory[]{new BasicInventory(10, 4) , new InventoryLocalCreativeMenu()}));
	 * // new Inventory(null, 10, 4, "La chatte à ta mère") if (k ==
	 * Keyboard.KEY_D) mainScene.changeOverlay(new DeathOverlay(mainScene, this));
	 * 
	 * if (k == Keyboard.KEY_R) mainScene.changeOverlay(new
	 * MessageBoxOverlay(mainScene, this, "Error : error"));
	 * 
	 * if (k == Keyboard.KEY_C && Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
	 * //Fabricated crash throw new RuntimeException("Epic crash"); } return false;
	 * }
	 */
}
