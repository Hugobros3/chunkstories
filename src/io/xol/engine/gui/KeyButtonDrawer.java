package io.xol.engine.gui;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;

import org.lwjgl.input.Keyboard;

public class KeyButtonDrawer
{

	public static void drawButtonForKey(float posx, float posy, int k, int size)
	{
		String keyname = Keyboard.getKeyName(k);
		int width = FontRenderer2.getTextLengthUsingFont(size * 16, keyname,
				BitmapFont.EDITUNDO);
		ObjectRenderer.renderTexturedRotatedRect(posx, posy, 24 * size,
				48 * size, 0, 0, 0, 12 / 64f, 1, "gui/key");
		ObjectRenderer.renderTexturedRotatedRect(posx + width - 5, posy,
				(width) * size * 2f, 48 * size, 0, 12 / 64f, 0, 24 / 64f, 1,
				"gui/key");
		ObjectRenderer.renderTexturedRotatedRect(posx + width * 2 * size - 12,
				posy, 24 * size, 48 * size, 0, 52 / 64f, 0, 1, 1, "gui/key");
		FontRenderer2.drawTextUsingSpecificFont(posx - 6, posy - 12, 0,
				size * 32, keyname, BitmapFont.EDITUNDO);
	}

	public static void drawButtonForKeyCentered(float decx, float posy, int k,
			int size)
	{
		String keyname = Keyboard.getKeyName(k);
		int width = FontRenderer2.getTextLengthUsingFont(size * 16, keyname,
				BitmapFont.EDITUNDO);

		float posx = XolioWindow.frameW / 2 - decx - width / 2;
		ObjectRenderer.renderTexturedRotatedRect(posx, posy, 24 * size,
				48 * size, 0, 0, 0, 12 / 64f, 1, "gui/key");
		ObjectRenderer.renderTexturedRotatedRect(posx + width - 5, posy,
				(width) * size * 2f, 48 * size, 0, 12 / 64f, 0, 24 / 64f, 1,
				"gui/key");
		ObjectRenderer.renderTexturedRotatedRect(posx + width * 2 * size - 12,
				posy, 24 * size, 48 * size, 0, 52 / 64f, 0, 1, 1, "gui/key");
		FontRenderer2.drawTextUsingSpecificFont(posx - 6, posy - 12, 0,
				size * 32, keyname, BitmapFont.EDITUNDO);
	}

	public static void drawButtonForKeyRightSide(float decx, float posy, int k,
			int size)
	{
		String keyname = Keyboard.getKeyName(k);
		int width = FontRenderer2.getTextLengthUsingFont(size * 16, keyname,
				BitmapFont.EDITUNDO);

		float posx = XolioWindow.frameW - decx - width - 16;
		ObjectRenderer.renderTexturedRotatedRect(posx, posy, 24 * size,
				48 * size, 0, 0, 0, 12 / 64f, 1, "gui/key");
		ObjectRenderer.renderTexturedRotatedRect(posx + width - 5, posy,
				(width) * size * 2f, 48 * size, 0, 12 / 64f, 0, 24 / 64f, 1,
				"gui/key");
		ObjectRenderer.renderTexturedRotatedRect(posx + width * 2 * size - 12,
				posy, 24 * size, 48 * size, 0, 52 / 64f, 0, 1, 1, "gui/key");
		FontRenderer2.drawTextUsingSpecificFont(posx - 6, posy - 12, 0,
				size * 32, keyname, BitmapFont.EDITUNDO);
	}
}
