//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.elements;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.TextInputGuiElement;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.rendering.textures.Texture2D;

public class InputText extends FocusableGuiElement implements TextInputGuiElement
{
	private String text = "";

	private Font ttfFont;
	
	private boolean isTransparent = false;
	private boolean password = false;

	public InputText(Layer layer, int x, int y, int width) {
		this(layer, x, y, width, layer.getGameWindow().getFontRenderer().defaultFont());
	}
	
	public InputText(Layer layer, int x, int y, int width, Font font) {
		super(layer);
		xPosition = x;
		yPosition = y;
		this.width = width;
		this.height = 22;
		
		this.ttfFont = font;
	}
	
	protected int scale() {
		return layer.getGuiScale();
	}
	
	public boolean handleInput(Input input) {
		if (input.equals("backspace")) {
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

	@Override
	public float getHeight() {
		return height * scale();
	}
	
	public float getWidth() {
		float len = width;
		int txtlen = ttfFont.getWidth(text);
		if(txtlen > len)
			len = txtlen;
		
		return len * scale();
	}
	
	public boolean isMouseOver(Mouse mouse)
	{
		return (mouse.getCursorX() >= xPosition && mouse.getCursorX() < xPosition + getWidth() 
		&& mouse.getCursorY() >= yPosition && mouse.getCursorY() <= yPosition + getHeight());
	}

	@Override
	public void render(RenderingInterface renderer) {
		
		String text = this.text;
		if(password) {
			String passworded = "";
			for (@SuppressWarnings("unused") char c : text.toCharArray())
				passworded += "*";
			text = passworded;
		}
		
		Texture2D backgroundTexture = renderer.textures().getTexture(isFocused() ? "./textures/gui/textbox.png" : "./textures/gui/textboxnofocus.png");
		if(this.isTransparent)
			backgroundTexture = renderer.textures().getTexture(isFocused() ? "./textures/gui/textboxnofocustransp.png" : "./textures/gui/textboxtransp.png");
			
		backgroundTexture.setLinearFiltering(false);
		
		renderer.getGuiRenderer().drawCorneredBoxTiled(xPosition - 0 * scale(), yPosition - 0 * scale(), getWidth() + 0 * scale(), getHeight() + 0 * scale(), 4, backgroundTexture, 32, scale());
		renderer.getFontRenderer().drawStringWithShadow(ttfFont, xPosition + 4 * scale(), yPosition + 1 * scale(), text + ((isFocused() && System.currentTimeMillis() % 1000 > 500) ? "|" : ""), scale(), scale(), new Vector4f(1.0f));
	
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public boolean isTransparent() {
		return isTransparent;
	}

	public void setTransparent(boolean isTransparent) {
		this.isTransparent = isTransparent;
	}

	public boolean isPassword() {
		return password;
	}

	public void setPassword(boolean password) {
		this.password = password;
	}
}
