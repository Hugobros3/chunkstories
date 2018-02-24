//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays.config;

import io.xol.chunkstories.api.gui.Layer;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay.ConfigButtonKey;

public class KeyBindSelectionOverlay extends Layer
{
	final ConfigButtonKey callback;
	
	public KeyBindSelectionOverlay(GameWindow scene, Layer options, ConfigButtonKey configButtonKey)
	{
		super(scene, options);
		callback = configButtonKey;
	}

	@Override
	public void render(RenderingInterface renderer)
	{
		this.parentLayer.render(renderer);
		
		renderer.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(), renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		
		String plz = "Please press a key";

		Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
		renderer.getFontRenderer().drawStringWithShadow(font, renderer.getWindow().getWidth() / 2 - font.getWidth(plz) * 1.5f, renderer.getWindow().getHeight() /2, plz, 3, 3, new Vector4f(1));
	}
	
	public void setKeyTo(int k) {
		callback.callBack(k);
		gameWindow.setLayer(parentLayer);
	}
}
