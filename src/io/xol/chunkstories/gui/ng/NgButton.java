package io.xol.chunkstories.gui.ng;

import org.lwjgl.input.Mouse;

import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.fonts.TrueTypeFontRenderer;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.GuiElement;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class NgButton extends GuiElement
{
	public boolean clicked = false;
	public String text;
	protected BitmapFont font;
	public int size;

	protected int height;

	public NgButton(int x, int y, String text)
	{
		this.posx = x;
		this.posy = y;
		this.text = text;
		this.height = 18;
	}

	public int getWidth()
	{
		//System.out.println(size);
		int width = FontRenderer2.getTextLengthUsingFont(size * 16, text, font);
		return width + 0;
	}

	public boolean isMouseOver()
	{
		int width = 0;
		return (Mouse.getX() >= posx - width / 2 - 4 && Mouse.getX() < posx + width / 2 + 4 && Mouse.getY() >= posy - height / 2 - 4 && Mouse.getY() <= posy + height / 2 + 4);
	}

	public void draw()
	{
		int width = TrueTypeFont.arial9f.getWidth(text);
		
		//int textDekal = -width / 2;
		if (hasFocus() || isMouseOver())
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 4, "./textures/gui/scalableButtonOver.png", 32, 1);
		}
		else
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy - 64, (int) (126 + 0 * Math.sin((154871 % 50000) / 500f)), 128, 4, "./textures/gui/scalableButtonOver.png", 32, 1);
		}
		
		TrueTypeFontRenderer.get().drawString(TrueTypeFont.arial9f, posx, posy, "I fucking love memes Singleplayer", 1);
		//FontRenderer2.drawTextUsingSpecificFont(0 + posx, posy - height / 2, 0, size * 32, text, font);
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
