package io.xol.engine.gui.elements;

import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.TextInputGuiElement;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InputText extends FocusableGuiElement implements TextInputGuiElement
{
	public String text = "";
	public int fontSize = 32;

	public BitmapFont font;
	//public float maxlen = 128;

	public InputText(Layer layer, int x, int y, int width, int fontSize, BitmapFont f)
	{
		super(layer);
		xPosition = x;
		yPosition = y;
		font = f;
		this.fontSize = fontSize;
		//this.maxlen = maxlen;
		this.width = width;
		this.height = 32;
	}
	
	protected int scale() {
		return layer.getGuiScale();
	}
	
	public boolean handleInput(Input input) {
		if(input.equals("backspace"))
		{
			if (text.length() > 0)
				text = text.substring(0, text.length() - 1);
			return true;
		}
		return false;
	}

	@Override
	public boolean handleTextInput(char c) {
		
		if (c != 0)
			text += c;
		
		return true;
	}

	public void drawWithBackGround()
	{
		float len = width;
		int txtlen = FontRenderer2.getTextLengthUsingFont(fontSize, text+" ", font);
		if(txtlen > len)
			len = txtlen;
		if (isFocused())
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + fontSize / 2, len, 32, 8, "./textures/gui/textbox.png");
		else
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + fontSize / 2, len, 32, 8, "./textures/gui/textboxnofocus.png");
		FontRenderer2.drawTextUsingSpecificFont(xPosition, yPosition, 0, fontSize, text + ((isFocused() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);
		// System.out.println(text);
	}

	public void drawWithBackGroundTransparent()
	{
		float len = width;
		int txtlen = FontRenderer2.getTextLengthUsingFont(fontSize, text+" ", font);
		if(txtlen > len)
			len = txtlen;
		if (isFocused())
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + fontSize / 2, len, 32, 8, "./textures/gui/textboxtransp.png");
		else
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + fontSize / 2, len, 32, 8, "./textures/gui/textboxnofocustransp.png");
		FontRenderer2.drawTextUsingSpecificFont(xPosition, yPosition, 0, fontSize, text + ((isFocused() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);
		// System.out.println(text);
	}
	
	public float getWidth() {
		float len = width;
		int txtlen = FontRenderer2.getTextLengthUsingFont(fontSize, text+" ", font);
		if(txtlen > len)
			len = txtlen;
		
		return len;
	}

	public void drawWithBackGroundPassworded()
	{
		String passworded = "";
		for (@SuppressWarnings("unused")
		char c : text.toCharArray())
			passworded += "*";
		if (isFocused())
			CorneredBoxDrawer.drawCorneredBox(xPosition + getWidth() / 2, yPosition + fontSize / 2, getWidth(), 32, 8, "./textures/gui/textbox.png");
		else
			CorneredBoxDrawer.drawCorneredBox(xPosition + getWidth() / 2, yPosition + fontSize / 2, getWidth(), 32, 8, "./textures/gui/textboxnofocus.png");
		FontRenderer2.drawTextUsingSpecificFont(xPosition, yPosition, 0, fontSize, passworded + ((isFocused() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), font, 1f);

	}

	public void setText(String t)
	{
		text = t;
	}
	
	public boolean isMouseOver(Mouse mouse)
	{
		return (mouse.getCursorX() >= xPosition - 4 && mouse.getCursorX() < xPosition + getWidth() + 4 && mouse.getCursorY() >= yPosition - 4 && mouse.getCursorY() <= yPosition + fontSize + 4);
	}

	@Override
	public void render(RenderingInterface renderer) {
		// TODO Auto-generated method stub
		
	}
}
