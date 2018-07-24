//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;

public class MessageBox extends Layer {
	BaseButton okButton = new BaseButton(this, 0, 0, 150, "#{menu.ok}");
	String message;

	public MessageBox(GameWindow scene, Layer parent, String message) {
		super(scene, parent);
		// Thread.dumpStack();
		// Gui buttons
		this.message = message;

		this.okButton.setAction(new Runnable() {

			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}

		});

		elements.add(okButton);
	}

	@Override
	public void render(RenderingInterface renderer) {
		parentLayer.render(renderer);

		renderer.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(),
				renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));

		float dekal = renderer.getFontRenderer().defaultFont().getWidth(message);
		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
				renderer.getWindow().getWidth() / 2 - dekal * 1.5f, renderer.getWindow().getHeight() / 2 + 64, message,
				3f, 3f, new Vector4f(1, 0.2f, 0.2f, 1));

		okButton.setPosition(renderer.getWindow().getWidth() / 2 - okButton.getWidth() / 2,
				renderer.getWindow().getHeight() / 2 - 32);
		okButton.render(renderer);
	}
}
