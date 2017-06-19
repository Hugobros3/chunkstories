package io.xol.engine.gui;

import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.gui.elements.Button;

import org.lwjgl.input.Mouse;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class LocalWorldButton extends Button
{
	int posx;
	int posy;

	public WorldInfoImplementation info;

	public int width, height;

	public LocalWorldButton(int x, int y, WorldInfoImplementation info)
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

	@Override
	public int draw()
	{
		if (hasFocus() || isMouseOver())
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 8, "./textures/gui/scalableButtonOver.png", 32, 2);
		}
		else
		{
			CorneredBoxDrawer.drawCorneredBoxTiled(posx, posy, width, height, 8, "./textures/gui/scalableButton.png", 32, 2);
		}
		
		//System.out.println(GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");
		ObjectRenderer.renderTexturedRect(posx - width / 2 + 32 + 4, posy, 64, 64, GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");

		//System.out.println("a+"+GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");
		
		FontRenderer2.setLengthCutoff(true, width - 72);
		FontRenderer2.drawTextUsingSpecificFont(posx - width / 2 + 72, posy, 0, 1 * 32, info.getName() + "#CCCCCC    Size : " + info.getSize().toString() + " ( " + info.getSize().sizeInChunks / 32 + "x" + info.getSize().sizeInChunks / 32 + " km )", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFontRVBA(posx - width / 2 + 72, posy - 32, 0, 1 * 32, info.getDescription(), BitmapFont.SMALLFONTS, 1.0f, 0.8f, 0.8f, 0.8f);
		FontRenderer2.setLengthCutoff(false, -1);
		return width * 2 - 12;
	}

	@Override
	public void setPosition(float f, float g)
	{
		posx = (int) f;
		posy = (int) g;
	}

	@Override
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
