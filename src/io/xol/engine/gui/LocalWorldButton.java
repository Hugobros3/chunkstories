package io.xol.engine.gui;

import io.xol.chunkstories.world.WorldInfo;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;

import org.lwjgl.input.Mouse;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class LocalWorldButton extends ClickableButton
{

	int posx;
	int posy;

	public WorldInfo info;

	public int width, height;

	public LocalWorldButton(int x, int y, WorldInfo info)
	{
		super(x, y, 0, 0, "", null, 333);
		posx = x;
		posy = y;
		this.info = info;
	}

	@Override
	public boolean isMouseOver()
	{
		return (Mouse.getX() >= posx - width / 2 - 4 && Mouse.getX() < posx + width / 2 + 4 && Mouse.getY() >= posy - height / 2 - 4 && Mouse.getY() <= posy + height / 2 + 4);
	}

	public int draw()
	{
		if (focus || isMouseOver())
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 8, "gui/scalableButtonOver", 32, 2);
		}
		else
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 8, "gui/scalableButton", 32, 2);
		}
		// ObjectRenderer.renderTexturedRect(posx, posy, 64, 64,
		// "internal://res/textures/gui/info.png");
		// if(info.hasLogo)
		ObjectRenderer.renderTexturedRect(posx - width / 2 + 32 + 4, posy, 64, 64, "gameDir://worlds/" + info.internalName + "/info.png");

		FontRenderer2.setLengthCutoff(true, width - 72);
		FontRenderer2.drawTextUsingSpecificFont(posx - width / 2 + 72, posy, 0, 1 * 32, info.name + "#CCCCCC    Size : " + info.size.toString() + " ( " + info.size.sizeInChunks / 32 + "x" + info.size.sizeInChunks / 32 + " km )", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFontRVBA(posx - width / 2 + 72, posy - 32, 0, 1 * 32, info.description, BitmapFont.SMALLFONTS, 1.0f, 0.8f, 0.8f, 0.8f);
		FontRenderer2.setLengthCutoff(false, -1);
		return width * 2 - 12;
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
