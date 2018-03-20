//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.ng;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.rendering.textures.Texture2D;

public class LargeButton extends BaseNgButton{
	
	public LargeButton(Layer layer, String text) {
		super(layer, layer.getGameWindow().getFontRenderer().getFont("LiberationSansNarrow-Bold__aa", 18.666f), 0, 0, text);
		this.width = 96;
		this.height = 24;

		this.text = "#{menu."+text+"}";
	}

	@Override
	public void render(RenderingInterface renderer) {
		String localizedText = layer.getGameWindow().getClient().getContent().localization().localize(text);
		
		Texture2D buttonTexture = renderer.textures().getTexture("./textures/gui/mainMenu.png");
		if (isFocused() || isMouseOver())
			buttonTexture = renderer.textures().getTexture("./textures/gui/mainMenuOver.png");
			
		buttonTexture.setLinearFiltering(false);
		renderer.getGuiRenderer().drawCorneredBoxTiled(xPosition, yPosition, getWidth(), getHeight(), 4 * scale(), buttonTexture, 32, scale());
		
		Font font = layer.getGameWindow().getRenderingInterface().getFontRenderer().getFont("LiberationSansNarrow-Bold__aa", 16f * scale());
		float a = 1f / scale();
		
		float yPositionText = yPosition + 2.5f * scale();
		float centering = getWidth() / 2 - font.getWidth(localizedText) * a * scale() / 2;
		renderer.getFontRenderer().drawString(font, xPosition + centering + scale(), yPositionText - scale(), localizedText, a*scale(), new Vector4f(161/255f, 161/255f, 161/255f, 1));
		renderer.getFontRenderer().drawString(font, xPosition + centering, yPositionText, localizedText, a*scale(), new Vector4f(38/255f, 38/255f, 38/255f, 1));
	}

}
