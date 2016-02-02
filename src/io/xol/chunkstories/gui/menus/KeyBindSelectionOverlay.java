package io.xol.chunkstories.gui.menus;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.menus.OptionsOverlay.ConfigButtonKey;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;

public class KeyBindSelectionOverlay extends Overlay
{
	ConfigButtonKey callback;
	
	public KeyBindSelectionOverlay(OverlayableScene scene, OptionsOverlay options, ConfigButtonKey configButtonKey)
	{
		super(scene, options);
		callback = configButtonKey;
	}

	public void drawToScreen(int x, int y, int w, int h)
	{
		parent.drawToScreen(x, y, w, h);
		ObjectRenderer.renderColoredRect(XolioWindow.frameW / 2, XolioWindow.frameH / 2, XolioWindow.frameW, XolioWindow.frameH, 0, "000000", 0.5f);
		int dekal = FontRenderer2.getTextLengthUsingFont(48, "Please press a key", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - dekal/2, XolioWindow.frameH /2, 0, 48, "Please press a key", BitmapFont.SMALLFONTS);
	}
	
	public boolean handleKeypress(int k)
	{
		callback.callBack(k);
		parent.mainScene.changeOverlay(parent);
		return true;
	}
}
