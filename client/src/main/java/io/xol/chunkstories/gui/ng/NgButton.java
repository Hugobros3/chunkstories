package io.xol.chunkstories.gui.ng;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.client.Client;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.fonts.TrueTypeFontRenderer;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.gui.elements.GuiElement;

//(c) 2015-2017 XolioWare Interactive
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
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		int width = GameWindowOpenGL.getInstance().renderingContext.getFontRenderer().getFont("arial", 12).getWidth(localizedText);
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
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		
		Texture2D buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButton2.png");
		if (hasFocus() || isMouseOver())
			buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButtonOver2.png");
			
		buttonTexture.setLinearFiltering(false);
		CorneredBoxDrawer.drawCorneredBoxTiled(posx + (width) / 2, posy + 9 * scale, width, 18 * scale, 4 * scale, buttonTexture, 32, scale);
		
		//if(scale == 1)
		GameWindowOpenGL.getInstance().renderingContext.getFontRenderer().drawString(GameWindowOpenGL.getInstance().renderingContext.getFontRenderer().getFont("arial", 12), posx + 4 * scale, posy, localizedText, scale, new Vector4fm(76/255f, 76/255f, 76/255f, 1));
		//else
		//	TrueTypeFontRenderer.get().drawString(TrueTypeFont.arial24px18pt, posx + 4 * scale, posy + 2, text, scale / 2, new Vector4fm(76/255f, 76/255f, 76/255f, 1));
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
