package io.xol.engine.gui;

import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;

import org.lwjgl.input.Mouse;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ClickableButton extends Focusable
{

	protected int posx;
	protected int posy;
	public boolean clicked = false;
	public String text = "";
	protected BitmapFont font;
	public int size;

	protected int width, height;

	public ClickableButton(int x, int y, int width, int height, String t, BitmapFont f, int s)
	{
		posx = x;
		posy = y;
		text = t;
		font = f;
		size = s;
		this.width = width;
		this.height = height;
	}

	public int getWidth()
	{
		//System.out.println(size);
		int width = FontRenderer2.getTextLengthUsingFont(size * 16, text, font);
		return width + 0;
	}

	public boolean isMouseOver()
	{
		return (Mouse.getX() >= posx - width / 2 - 4 && Mouse.getX() < posx + width / 2 + 4 && Mouse.getY() >= posy - height / 2 - 4 && Mouse.getY() <= posy + height / 2 + 4);
	}

	public int draw()
	{
		int textWidth = FontRenderer2.getTextLengthUsingFont(size * 16, text, font);
		if (width < 0)
		{
			width = textWidth;
		}
		int textDekal = -textWidth;
		if (focus || isMouseOver())
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 8, "gui/scalableButtonOver", 32, 2);
		}
		else
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 8, "gui/scalableButton", 32, 2);
		}
		FontRenderer2.drawTextUsingSpecificFont(textDekal + posx, posy - height / 2, 0, size * 32, text, font);
		return width * 2 * size - 12;
	}

	public void setPos(float f, float g)
	{
		posx = (int) f;
		posy = (int) g;
	}

	public boolean clicked()
	{
		if (clicked)
		{
			clicked = false;
			return true;
		}
		return false;
	}
}
