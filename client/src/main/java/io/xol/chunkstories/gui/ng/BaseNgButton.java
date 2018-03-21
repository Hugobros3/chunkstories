//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.ng;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.ClickableGuiElement;
import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.rendering.textures.Texture2D;

public class BaseNgButton extends FocusableGuiElement implements ClickableGuiElement {
	public String text;
	public Font font;
	
	private Runnable action;
	
	public BaseNgButton(Layer layer, int x, int y, String text) {
		this(layer, x, y, text, null);
	}
	
	public BaseNgButton(Layer layer, int x, int y, int width, String text) {
		this(layer, x, y, text, null);
		this.setWidth(width);
	}

	public BaseNgButton(Layer layer, int x, int y, String text, Runnable action) {
		this(layer, layer.getGameWindow().getFontRenderer().getFont("LiberationSans-Regular", 12), x, y, text);
		this.action = action;
	}

	public BaseNgButton(Layer layer, Font font, int x, int y, String text) {
		super(layer);
		this.font = font;
		
		this.xPosition = x;
		this.yPosition = y;
		this.text = text;
		this.height = 24;
	}
	
	protected int scale() {
		return layer.getGuiScale();
	}

	public float getWidth()
	{
		String localizedText = layer.getGameWindow().getClient().getContent().localization().localize(text);
		float width = font.getWidth(localizedText) + 8;
		
		if(this.width > width)
			width = this.width;
		
		return (width) * scale();
	}
	
	public float getHeight() {
		return height * scale();
	}

	public boolean isMouseOver(Mouse mouse) {
		return (mouse.getCursorX() >= xPosition && mouse.getCursorX() < xPosition + getWidth()
				&& mouse.getCursorY() >= yPosition && mouse.getCursorY() <= yPosition + getHeight());
	}

	@Override
	public void render(RenderingInterface renderer) {
		float width = getWidth();
		String localizedText = layer.getGameWindow().getClient().getContent().localization().localize(text);
		
		Texture2D buttonTexture = renderer.textures().getTexture("./textures/gui/scalableButton2.png");
		if (isFocused() || isMouseOver())
			buttonTexture = renderer.textures().getTexture("./textures/gui/scalableButtonOver2.png");
			
		buttonTexture.setLinearFiltering(false);
		renderer.getGuiRenderer().drawCorneredBoxTiled(xPosition, yPosition, width, getHeight(), 4 * scale(), buttonTexture, 32, scale());
		
		renderer.getFontRenderer().drawString(font, xPosition + 4 * scale(), yPosition, localizedText, scale(), new Vector4f(76/255f, 76/255f, 76/255f, 1));
	}

	@Override
	public boolean handleClick(MouseButton mouseButton) {
		if(!mouseButton.equals("mouse.left"))
			return false;
		
		this.layer.getGameWindow().getClient().getSoundManager().playSoundEffect("./sounds/gui/gui_click2.ogg");
		
		if(this.action != null)
			this.action.run();
		
		return true;
	}

	public void setAction(Runnable runnable) {
		this.action = runnable;
	}
}
