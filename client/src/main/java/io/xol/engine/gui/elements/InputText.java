package io.xol.engine.gui.elements;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.TextInputGuiElement;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InputText extends FocusableGuiElement implements TextInputGuiElement
{
	public String text = "";
	public int padding = 32;

	private Font ttfFont;

	public InputText(Layer layer, int x, int y, int width) {
		this(layer, x, y, width, Client.getInstance().getContent().fonts().defaultFont());
	}
	
	public InputText(Layer layer, int x, int y, int width, Font font)
	{
		super(layer);
		xPosition = x;
		yPosition = y;
		this.padding = 32;
		this.width = width;
		this.height = 32;
		
		this.ttfFont = font;
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

	public void drawWithBackGround(RenderingInterface renderer)
	{
		float len = width;
		int txtlen = ttfFont.getWidth(text) * scale();
		if(txtlen > len)
			len = txtlen;
		if (isFocused())
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + padding / 2, len, 32, 8, "./textures/gui/textbox.png");
		else
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + padding / 2, len, 32, 8, "./textures/gui/textboxnofocus.png");
		
		
		renderer.getFontRenderer().drawStringWithShadow(ttfFont, xPosition + scale(), yPosition - scale(), text + ((isFocused() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), scale(), scale(), new Vector4f(1.0f));
	}

	public void drawWithBackGroundTransparent(RenderingInterface renderer)
	{
		float len = width;
		int txtlen = ttfFont.getWidth(text) * scale();
		if(txtlen > len)
			len = txtlen;
		if (isFocused())
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + padding / 2, len, 32, 8, "./textures/gui/textboxtransp.png");
		else
			CorneredBoxDrawer.drawCorneredBox(xPosition + len / 2, yPosition + padding / 2, len, 32, 8, "./textures/gui/textboxnofocustransp.png");
		
		renderer.getFontRenderer().drawStringWithShadow(ttfFont, xPosition + scale(), yPosition - scale(), text + ((isFocused() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), scale(), scale(), new Vector4f(1.0f));
	}
	
	public float getWidth() {
		float len = width;
		int txtlen = ttfFont.getWidth(text) * scale();
		if(txtlen > len)
			len = txtlen;
		
		return len;
	}

	public void drawWithBackGroundPassworded(RenderingInterface renderer)
	{
		String passworded = "";
		for (@SuppressWarnings("unused")
		char c : text.toCharArray())
			passworded += "*";
		if (isFocused())
			CorneredBoxDrawer.drawCorneredBox(xPosition + getWidth() / 2, yPosition + padding / 2, getWidth(), 32, 8, "./textures/gui/textbox.png");
		else
			CorneredBoxDrawer.drawCorneredBox(xPosition + getWidth() / 2, yPosition + padding / 2, getWidth(), 32, 8, "./textures/gui/textboxnofocus.png");
		
		renderer.getFontRenderer().drawStringWithShadow(ttfFont, xPosition + scale(), yPosition - scale(), passworded + ((isFocused() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), scale(), scale(), new Vector4f(1.0f));
	}

	public void setText(String t)
	{
		text = t;
	}
	
	public boolean isMouseOver(Mouse mouse)
	{
		return (mouse.getCursorX() >= xPosition - 4 && mouse.getCursorX() < xPosition + getWidth() + 4 && mouse.getCursorY() >= yPosition - 4 && mouse.getCursorY() <= yPosition + padding + 4);
	}

	@Override
	public void render(RenderingInterface renderer) {
		// TODO Auto-generated method stub
		
	}
}
