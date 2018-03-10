//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.ng;

import io.xol.chunkstories.api.gui.Layer;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

public class LargeButtonIcon extends BaseNgButton{

	String iconName;
	
	public LargeButtonIcon(Layer layer, String text) {
		super(layer, Client.getInstance().getGameWindow().getRenderingContext().getFontRenderer().getFont("LiberationSansNarrow-Bold__aa", 16f), 0, 0, text);
		this.width = 96;
		this.height = 48;
		
		this.iconName = text;
		this.text = "#{menu."+text+"}";
	}

	@Override
	public void render(RenderingInterface renderer) {
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		
		Texture2D buttonTexture = TexturesHandler.getTexture("./textures/gui/mainMenu.png");
		if (isFocused() || isMouseOver())
			buttonTexture = TexturesHandler.getTexture("./textures/gui/mainMenuOver.png");
			
		buttonTexture.setLinearFiltering(false);
		CorneredBoxDrawer.drawCorneredBoxTiled(xPosition + getWidth() / 2, yPosition + getHeight() / 2, getWidth(), getHeight(), 4 * scale(), buttonTexture, 32, scale());
		
		Font font = Client.getInstance().getGameWindow().getRenderingContext().getFontRenderer().getFont("LiberationSansNarrow-Bold__aa", 32f);
		float a = 0.5f;
		//a = 1;
		
		float yPositionText = yPosition + 26 * scale();
		float centering = getWidth() / 2 - font.getWidth(localizedText) * a * scale() / 2;
		renderer.getFontRenderer().drawString(font, xPosition + centering + scale(), yPositionText - scale(), localizedText, a * scale(), new Vector4f(161/255f, 161/255f, 161/255f, 1));
		renderer.getFontRenderer().drawString(font, xPosition + centering, yPositionText, localizedText, a * scale(), new Vector4f(38/255f, 38/255f, 38/255f, 1));
	
		renderer.textures().getTexture("./textures/gui/icons/"+iconName+".png").setLinearFiltering(false);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(xPosition + getWidth() / 2 - 16 * scale(), yPosition + getHeight() / 2 - 26 * scale(), 32 * scale(), 32 * scale(), 0, 1, 1, 0, renderer.textures().getTexture("./textures/gui/icons/"+iconName+".png"), false, true, null);
	}

}
