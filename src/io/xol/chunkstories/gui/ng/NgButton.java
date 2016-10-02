package io.xol.chunkstories.gui.ng;

import org.lwjgl.input.Mouse;

import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.fonts.TrueTypeFontRenderer;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.gui.elements.GuiElement;
import io.xol.engine.math.lalgb.Vector4f;

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
		int width = TrueTypeFont.arial12px9pt.getWidth(text);
		return (width + 8) * scale;
	}

	public boolean isMouseOver()
	{
		int width = getWidth();
		return (Mouse.getX() >= posx && Mouse.getX() < posx + width && Mouse.getY() >= posy && Mouse.getY() <= posy + height * scale);
	}

	public void draw()
	{
		int width = getWidth();
		
		Texture2D buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButton2.png");
		if (hasFocus() || isMouseOver())
			buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButtonOver2.png");
			
		buttonTexture.setLinearFiltering(false);
		CorneredBoxDrawer.drawCorneredBoxTiled(posx + (width) / 2, posy + 9 * scale, width, 18 * scale, 4 * scale, buttonTexture, 32, scale);
		
		//if(scale == 1)
			TrueTypeFontRenderer.get().drawString(TrueTypeFont.arial12px9pt, posx + 4 * scale, posy, text, scale, new Vector4f(76/255f, 76/255f, 76/255f, 1));
		//else
		//	TrueTypeFontRenderer.get().drawString(TrueTypeFont.arial24px18pt, posx + 4 * scale, posy + 2, text, scale / 2, new Vector4f(76/255f, 76/255f, 76/255f, 1));
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
