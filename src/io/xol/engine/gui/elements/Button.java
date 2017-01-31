package io.xol.engine.gui.elements;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Button extends GuiElement
{
	public boolean clicked = false;
	public String text = "";
	protected BitmapFont font;
	public int size;

	protected int width, height;

	public Button(int x, int y, int width, int height, String t, BitmapFont f, int s)
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
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		int textWidth = FontRenderer2.getTextLengthUsingFont(size * 16, localizedText, font);
		if (width < 0)
		{
			width = textWidth;
		}
		int textDekal = -textWidth;
		if (hasFocus() || isMouseOver())
		{
			TexturesHandler.getTexture("./textures/gui/scalableButtonOver.png").setLinearFiltering(false);
			CorneredBoxDrawer.drawCorneredBoxTiled(posx - 4, posy, width + 8, height + 16, 4, "./textures/gui/scalableButtonOver.png", 32, 2);
		}
		else
		{
			TexturesHandler.getTexture("./textures/gui/scalableButton.png").setLinearFiltering(false);
			CorneredBoxDrawer.drawCorneredBoxTiled(posx - 4, posy, width + 8, height + 16, 4, "./textures/gui/scalableButton.png", 32, 2);
		}
		FontRenderer2.drawTextUsingSpecificFont(textDekal + posx, posy - height / 2, 0, size * 32, localizedText, font);
		return width * 2 * size - 12;
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
