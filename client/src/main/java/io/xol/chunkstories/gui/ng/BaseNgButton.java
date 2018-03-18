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
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.opengl.texture.TexturesHandler;
import io.xol.chunkstories.renderer.opengl.util.CorneredBoxDrawer;

public class BaseNgButton extends FocusableGuiElement implements ClickableGuiElement
{
	public String text;
	public Font font;
	
	private Runnable action;
	
	public BaseNgButton(Layer layer, int x, int y, String text)
	{
		this(layer, Client.getInstance().getGameWindow().getRenderingContext().getFontRenderer().getFont("LiberationSans-Regular", 12), x, y, text);
	}
	
	public BaseNgButton(Layer layer, Font font, int x, int y, String text)
	{
		super(layer);
		this.font = font;
		
		this.xPosition = x;
		this.yPosition = y;
		this.text = text;
		this.height = 32;
	}
	
	protected int scale() {
		return layer.getGuiScale();
	}

	public float getWidth()
	{
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		float width = font.getWidth(localizedText);
		
		if(this.width > width)
			width = this.width;
		
		return (width) * scale();
	}
	
	public float getHeight() {
		return height * scale();
	}

	public boolean isMouseOver(Mouse mouse)
	{
		return (mouse.getCursorX() >= xPosition && mouse.getCursorX() < xPosition + getWidth() && mouse.getCursorY() >= yPosition && mouse.getCursorY() <= yPosition + getHeight());
	}

	@Override
	public void render(RenderingInterface renderer) {
		float width = getWidth();
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		
		Texture2D buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButton2.png");
		if (isFocused() || isMouseOver())
			buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButtonOver2.png");
			
		buttonTexture.setLinearFiltering(false);
		CorneredBoxDrawer.drawCorneredBoxTiled(xPosition + (width) / 2, yPosition + getHeight() / 2, width, getHeight(), 4 * scale(), buttonTexture, 32, scale());
		
		//if(scale == 1)
		renderer.getFontRenderer().drawString(font, xPosition + 4 * scale(), yPosition, localizedText, scale(), new Vector4f(76/255f, 76/255f, 76/255f, 1));
		//else
		//	TrueTypeFontRenderer.get().drawString(TrueTypeFont.arial24px18pt, posx + 4 * scale, posy + 2, text, scale / 2, new Vector4f(76/255f, 76/255f, 76/255f, 1));
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
