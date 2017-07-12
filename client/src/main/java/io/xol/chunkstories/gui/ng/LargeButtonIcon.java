package io.xol.chunkstories.gui.ng;

import io.xol.chunkstories.api.gui.Layer;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LargeButtonIcon extends BaseNgButton{

	String iconName;
	
	public LargeButtonIcon(Layer layer, String text) {
		super(layer, Client.getInstance().getGameWindow().getRenderingContext().getFontRenderer().getFont("haettenschweiler", 18.666f), 0, 0, text);
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
		
		float yPositionText = yPosition + 50 * scale() / 2;
		float centering = getWidth() / 2 - font.getWidth(localizedText) * scale() / 2;
		renderer.getFontRenderer().drawString(font, xPosition + centering + scale(), yPositionText - scale(), localizedText, scale(), new Vector4f(161/255f, 161/255f, 161/255f, 1));
		renderer.getFontRenderer().drawString(font, xPosition + centering, yPositionText, localizedText, scale(), new Vector4f(38/255f, 38/255f, 38/255f, 1));
	
		renderer.textures().getTexture("./textures/gui/icons/"+iconName+".png").setLinearFiltering(false);
		renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(xPosition + getWidth() / 2 - 16 * scale(), yPosition + getHeight() / 2 - 26 * scale(), 32 * scale(), 32 * scale(), 0, 1, 1, 0, renderer.textures().getTexture("./textures/gui/icons/"+iconName+".png"), false, true, null);
	}

}
