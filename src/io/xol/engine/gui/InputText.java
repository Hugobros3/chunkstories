package io.xol.engine.gui;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.xol.engine.font.BitmapFont;
import io.xol.engine.font.FontRenderer2;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InputText extends Focusable
{
	int posx;
	int posy;

	public String text = "";

	public static int charDelay = 10;

	public InputText(int x, int y, int maxlen, int fontSize, BitmapFont f)
	{
		posx = x;
		posy = y;
		font = f;
		this.fontSize = fontSize;
		this.maxlen = maxlen;
	}

	public void update()
	{
		if (focus)
		{
			while (Keyboard.next())
			{
				if (Keyboard.getEventKeyState() == true)
				{
					char c = Keyboard.getEventCharacter();
					int ek = Keyboard.getEventKey();
					if (ek == 14)
					{
						if (text.length() > 0)
							text = text.substring(0, text.length() - 1);
					}
					else if (ek == 28)
					{

					}
					else
					// if(TextKeys.isTextKey(ek))
					{
						if (c != 0)
							text += c;
					}
				}
			}
		}
	}

	public void input(int k)
	{
		char c = Keyboard.getEventCharacter();

		int ek = k;
		if (ek == 14)
		{
			if (text.length() > 0)
				text = text.substring(0, text.length() - 1);
		}
		else if (ek == 28)
		{

		}
		else
		// if(TextKeys.isTextKey(ek))
		{
			if (c != 0)
				text += c;
		}

		// System.out.println("passing input "+k+" c="+c+" txt="+text);
	}

	public void drawWithBackGround()
	{
		if (focus)
			CorneredBoxDrawer.drawCorneredBox(posx + maxlen / 2, posy + fontSize / 2, maxlen, 32, 8, "gui/textbox");
		else
			CorneredBoxDrawer.drawCorneredBox(posx + maxlen / 2, posy + fontSize / 2, maxlen, 32, 8, "gui/textboxnofocus");
		FontRenderer2.drawTextUsingSpecificFont(posx, posy, 0, fontSize, text + ((focus && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);
		// System.out.println(text);
	}

	public void drawWithBackGroundTransparent()
	{
		int len = maxlen;
		int txtlen = FontRenderer2.getTextLengthUsingFont(fontSize, text+" ", font);
		if(txtlen > len)
			len = txtlen;
		if (focus)
			CorneredBoxDrawer.drawCorneredBox(posx + len / 2, posy + fontSize / 2, len, 32, 8, "gui/textboxtransp");
		else
			CorneredBoxDrawer.drawCorneredBox(posx + len / 2, posy + fontSize / 2, len, 32, 8, "gui/textboxnofocustransp");
		FontRenderer2.drawTextUsingSpecificFont(posx, posy, 0, fontSize, text + ((focus && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);
		// System.out.println(text);
	}

	public void drawWithBackGroundPassworded()
	{
		String passworded = "";
		for (@SuppressWarnings("unused")
		char c : text.toCharArray())
			passworded += "*";
		if (focus)
			CorneredBoxDrawer.drawCorneredBox(posx + maxlen / 2, posy + fontSize / 2, maxlen, 32, 8, "gui/textbox");
		else
			CorneredBoxDrawer.drawCorneredBox(posx + maxlen / 2, posy + fontSize / 2, maxlen, 32, 8, "gui/textboxnofocus");
		FontRenderer2.drawTextUsingSpecificFont(posx, posy, 0, fontSize, passworded + ((focus && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);

	}

	public void setPos(float f, float g)
	{
		posx = (int) f;
		posy = (int) g;
	}

	public void setText(String t)
	{
		text = t;
	}

	public int fontSize = 32;

	public BitmapFont font;
	public int maxlen = 128;

	public void setMaxLength(int maxLength)
	{
		maxlen = maxLength;
	}
	
	public boolean isMouseOver()
	{
		return (Mouse.getX() >= posx - 4 && Mouse.getX() < posx + maxlen + 4 && Mouse.getY() >= posy - 4 && Mouse.getY() <= posy + fontSize + 4);
	}
}
