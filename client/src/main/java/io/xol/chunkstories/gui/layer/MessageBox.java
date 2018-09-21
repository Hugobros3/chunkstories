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
import io.xol.chunkstories.api.gui.elements.BaseButton;

public class MessageBox extends Layer {
	BaseButton okButton = new BaseButton(this, 0, 0, 150, "#{menu.ok}");
	String message;

	public MessageBox(Gui gui, Layer parent, String message) {
		super(gui, parent);
		this.message = message;

		this.okButton.setAction(new Runnable() {

			@Override
			public void run() {
				gui.setTopLayer(parentLayer);
			}

		});

		elements.add(okButton);
	}

	@Override
	public void render(GuiDrawer drawer) {
		parentLayer.render(drawer);

		drawer.drawBoxWindowsSpaceWithSize(0, 0, gui.getViewportWidth(), gui.getViewportHeight(), 0, 0, 0, 0, null, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));

		int dekal = drawer.getFonts().defaultFont().getWidth(message);
		drawer.drawStringWithShadow(drawer.getFonts().defaultFont(),
				gui.getViewportWidth() / 2 - dekal * 2, gui.getViewportHeight() / 2 + 64, message, -1, new Vector4f(1, 0.2f, 0.2f, 1));

		okButton.setPosition(gui.getViewportWidth() / 2 - okButton.getWidth() / 2,
				gui.getViewportHeight() / 2 - 32);
		okButton.render(drawer);
	}
}
