package io.xol.engine.gui.elements;

import io.xol.chunkstories.api.gui.ClickableGuiElement;
import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Button extends FocusableGuiElement implements ClickableGuiElement
{
	//public boolean clicked = false;
	public String text = "";
	protected BitmapFont font;
	public int size;

	//protected int width, height;
	private Runnable action;

	public Button(Layer layer, int x, int y, int width, int height, String t, BitmapFont f, int s)
	{
		this(layer, x, y, width, height, t, f, s, null);
	}
	
	public Button(Layer layer, int x, int y, int width, int height, String t, BitmapFont f, int s, Runnable r)
	{
		super(layer);
		xPosition = x;
		yPosition = y;
		text = t;
		font = f;
		size = s;
		this.width = width;
		this.height = height;
		
		this.action = r;
	}

	public float getWidth()
	{
		//System.out.println(size);
		int width = FontRenderer2.getTextLengthUsingFont(size * 16, text, font);
		return width + 0;
	}

	public boolean isMouseOver(Mouse mouse)
	{
		return (mouse.getCursorX() >= xPosition - width / 2 - 4 && mouse.getCursorX() < xPosition + width / 2 + 4 && mouse.getCursorY() >= yPosition - height / 2 - 4 && mouse.getCursorY() <= yPosition + height / 2 + 4);
	}

	public void render(RenderingInterface renderer) {
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		int textWidth = FontRenderer2.getTextLengthUsingFont(size * 16, localizedText, font);
		if (width < 0)
		{
			width = textWidth;
		}
		int textDekal = -textWidth;
		if (isFocused() || isMouseOver())
		{
			TexturesHandler.getTexture("./textures/gui/scalableButtonOver.png").setLinearFiltering(false);
			CorneredBoxDrawer.drawCorneredBoxTiled(xPosition - 4, yPosition, width + 8, height + 16, 4, "./textures/gui/scalableButtonOver.png", 32, 2);
		}
		else
		{
			TexturesHandler.getTexture("./textures/gui/scalableButton.png").setLinearFiltering(false);
			CorneredBoxDrawer.drawCorneredBoxTiled(xPosition - 4, yPosition, width + 8, height + 16, 4, "./textures/gui/scalableButton.png", 32, 2);
		}
		FontRenderer2.drawTextUsingSpecificFont(textDekal + xPosition, yPosition - height / 2, 0, size * 32, localizedText, font);
		//return width * 2 * size - 12;
	}

	@Override
	public boolean handleClick(MouseButton mouseButton) {
		if(!mouseButton.equals("mouse.left"))
			return false;
		
		if(this.action != null)
			this.action.run();
		
		return true;
	}

	public void setAction(Runnable runnable) {
		this.action = runnable;
	}
}
