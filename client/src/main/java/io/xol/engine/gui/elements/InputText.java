package io.xol.engine.gui.elements;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InputText extends GuiElement
{
	public String text = "";

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
		if (hasFocus())
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
	}

	public void drawWithBackGround()
	{
		int len = maxlen;
		int txtlen = FontRenderer2.getTextLengthUsingFont(fontSize, text+" ", font);
		if(txtlen > len)
			len = txtlen;
		if (hasFocus())
			CorneredBoxDrawer.drawCorneredBox(posx + len / 2, posy + fontSize / 2, len, 32, 8, "./textures/gui/textbox.png");
		else
			CorneredBoxDrawer.drawCorneredBox(posx + len / 2, posy + fontSize / 2, len, 32, 8, "./textures/gui/textboxnofocus.png");
		FontRenderer2.drawTextUsingSpecificFont(posx, posy, 0, fontSize, text + ((hasFocus() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);
		// System.out.println(text);
	}

	public void drawWithBackGroundTransparent()
	{
		int len = maxlen;
		int txtlen = FontRenderer2.getTextLengthUsingFont(fontSize, text+" ", font);
		if(txtlen > len)
			len = txtlen;
		if (hasFocus())
			CorneredBoxDrawer.drawCorneredBox(posx + len / 2, posy + fontSize / 2, len, 32, 8, "./textures/gui/textboxtransp.png");
		else
			CorneredBoxDrawer.drawCorneredBox(posx + len / 2, posy + fontSize / 2, len, 32, 8, "./textures/gui/textboxnofocustransp.png");
		FontRenderer2.drawTextUsingSpecificFont(posx, posy, 0, fontSize, text + ((hasFocus() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);
		// System.out.println(text);
	}

	public void drawWithBackGroundPassworded()
	{
		String passworded = "";
		for (@SuppressWarnings("unused")
		char c : text.toCharArray())
			passworded += "*";
		if (hasFocus())
			CorneredBoxDrawer.drawCorneredBox(posx + maxlen / 2, posy + fontSize / 2, maxlen, 32, 8, "./textures/gui/textbox.png");
		else
			CorneredBoxDrawer.drawCorneredBox(posx + maxlen / 2, posy + fontSize / 2, maxlen, 32, 8, "./textures/gui/textboxnofocus.png");
		FontRenderer2.drawTextUsingSpecificFont(posx, posy, 0, fontSize, passworded + ((hasFocus() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);

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
