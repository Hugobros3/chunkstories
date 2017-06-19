package io.xol.chunkstories.gui.overlays.config;

import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay.ConfigButtonKey;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.Overlay;

//(c) 2015-2017 XolioWare Interactive
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
		
		//ObjectRenderer.renderColoredRect(renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, "000000", 0.5f);
		//renderingContext.getGuiRenderer().renderColoredRect(renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, "000000", 0.5f);
		
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		
		int dekal = FontRenderer2.getTextLengthUsingFont(48, "Please press a key", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - dekal/2, renderingContext.getWindow().getHeight() /2, 0, 48, "Please press a key", BitmapFont.SMALLFONTS);
	}
	
	@Override
	public boolean handleKeypress(int k)
	{
		callback.callBack(k);
		parent.mainScene.changeOverlay(parent);
		return true;
	}
}
