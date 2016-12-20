package io.xol.chunkstories.gui.overlays.config;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay.ConfigButtonKey;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.math.lalgb.vector.sp.Vector4fm;
import io.xol.engine.base.GameWindowOpenGL;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class KeyBindSelectionOverlay extends Overlay
{
	ConfigButtonKey callback;
	
	public KeyBindSelectionOverlay(OverlayableScene scene, OptionsOverlay options, ConfigButtonKey configButtonKey)
	{
		super(scene, options);
		callback = configButtonKey;
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		parent.drawToScreen(renderingContext, x, y, w, h);
		
		//ObjectRenderer.renderColoredRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, "000000", 0.5f);
		//renderingContext.getGuiRenderer().renderColoredRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, "000000", 0.5f);
		
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		
		int dekal = FontRenderer2.getTextLengthUsingFont(48, "Please press a key", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - dekal/2, GameWindowOpenGL.windowHeight /2, 0, 48, "Please press a key", BitmapFont.SMALLFONTS);
	}
	
	@Override
	public boolean handleKeypress(int k)
	{
		callback.callBack(k);
		parent.mainScene.changeOverlay(parent);
		return true;
	}
}
