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
import io.xol.chunkstories.api.rendering.gui;
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

		this.largeSingleplayer.setAction(() -> gui.setTopLayer(new LevelSelection(gui, MainMenu.this)));
		this.largeOnline.setAction(() -> gui.setTopLayer(new ServerSelection(gui, MainMenu.this, false)));
		this.largeMods.setAction(() -> gui.setTopLayer(new ModsSelection(gui, MainMenu.this)));
		this.largeOptions.setAction(() -> gui.setTopLayer(new OptionsScreen(gui, MainMenu.this)));

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
	public void render(GuiDrawer drawer) {
		parentLayer.render(drawer);

		if (gui.getTopLayer() == this && gui.getClient().getConfiguration()
				.getStringOption("client.game.log-policy").equals("undefined"))
			gui.setTopLayer(new LogPolicyAsk(gui, this));

		int spacing = 4;
		int buttonsAreaSize = largeSingleplayer.getWidth() * 2 + spacing;

		int leftButtonX = this.getWidth() / 2 - buttonsAreaSize / 2;

		int ySmall = 12;
		int yBig = ySmall + largeSingleplayer.getHeight() + (spacing);

		largeOnline.setPosition(leftButtonX, yBig);
		largeOnline.render(drawer);

		largeSingleplayer.setPosition(leftButtonX, ySmall);
		largeSingleplayer.render(drawer);

		int rightButtonX = leftButtonX + largeSingleplayer.getWidth() + (spacing) * 1;

		largeMods.setPosition(rightButtonX, yBig);
		largeMods.render(drawer);

		largeOptions.setPosition(rightButtonX, ySmall);
		largeOptions.render(drawer);

		// Notices
		Vector4f noticeColor = new Vector4f(0.5f);
		String version = "Chunk Stories Client " + VersionInfo.version;
		drawer.getFonts().defaultFont().getWidth(version);
		drawer.drawString(drawer.getFonts().defaultFont(), 4, 0, version,
				1, noticeColor);

		String copyrightNotice = "https://github.com/Hugobros3/chunkstories";
		int noticeDekal = drawer.getFonts().defaultFont().getWidth(copyrightNotice);
		drawer.drawString(drawer.getFonts().defaultFont(),
				gui.getViewportWidth() - noticeDekal - 4, 0, copyrightNotice, 1,
				noticeColor);

	}

	@Override
	public boolean handleTextInput(char c) {
		if (c == 'e') {
			gui.setTopLayer(new InventoryView(gui, this, new Inventory[]{new BasicInventory(10, 4)}));
		} else if (c == 'd') {
			gui.setTopLayer(new DeathScreen(gui, this));
		} else if (c == 'r') {
			gui.setTopLayer(new MessageBox(gui, this, "Error : error"));
		} else if (c == 'l') {
			gui.setTopLayer(new LanguageSelectionScreen(gui, this, true));
		} else if (c == 'o') {
			gui.setTopLayer(new LogPolicyAsk(gui, this));
		} else if (c == 'c') {
			// Fabricated crash
			throw new RuntimeException("Epic crash");
		}

		return super.handleTextInput(c);
	}
}
