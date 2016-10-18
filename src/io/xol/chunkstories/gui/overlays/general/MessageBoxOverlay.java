package io.xol.chunkstories.gui.overlays.general;

import io.xol.engine.math.lalgb.Vector4f;
import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MessageBoxOverlay extends Overlay
{
	Button okButton = new Button(0, 0, 300, 32, ("Ok"), BitmapFont.SMALLFONTS, 1);
	String message;
	
	public MessageBoxOverlay(OverlayableScene scene, Overlay parent, String message)
	{
		super(scene, parent);
		// Gui buttons
		this.message = message;
		guiHandler.add(okButton);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		ObjectRenderer.renderTexturedRectAlpha(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight / 2 + 256, 1024, 1024, "logo", 1f);
		FontRenderer2.drawTextUsingSpecificFontRVBA(GameWindowOpenGL.windowWidth/2+192, GameWindowOpenGL.windowHeight / 2 + 160, 0, 48, "Indev " + VersionInfo.version, BitmapFont.SMALLFONTS, 1, 0.5f, 1, 1);

		int dekal = TrueTypeFont.arial11px.getWidth(message);
		renderingContext.getTrueTypeFontRenderer().drawStringWithShadow(TrueTypeFont.arial11px, GameWindowOpenGL.windowWidth/2-dekal*1.5f, GameWindowOpenGL.windowHeight / 2 + 64, message, 3f, 3f, new Vector4f(1,0.2f,0.2f,1));
		
		okButton.setPosition(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight / 2 - 32);
		okButton.draw();

		if (okButton.clicked())
			mainScene.changeOverlay(this.parent);
	}
}
