//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import io.xol.chunkstories.api.gui.Font;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.gui.layer.config.OptionsScreen.ConfigButtonKey;

public class KeyBindSelectionOverlay extends Layer {
	final ConfigButtonKey callback;

	public KeyBindSelectionOverlay(Gui gui, Layer options, ConfigButtonKey configButtonKey) {
		super(gui, options);
		callback = configButtonKey;
	}

	@Override
	public void render(GuiDrawer drawer) {
		this.parentLayer.render(drawer);

		drawer.drawBoxWindowsSpaceWithSize(0, 0, gui.getViewportWidth(), gui.getViewportHeight(), null, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));

		//TODO localization
		String plz = "Please press a key";

		Font font = drawer.getFonts().getFont("LiberationSans-Regular", 11);
		drawer.drawStringWithShadow(font, gui.getViewportWidth() / 2 - font.getWidth(plz) * 2, gui.getViewportHeight() / 2, plz, -1, new Vector4f(1));
	}

	public void setKeyTo(int k) {
		callback.callBack(k);
		gui.setTopLayer(parentLayer);
	}
}
