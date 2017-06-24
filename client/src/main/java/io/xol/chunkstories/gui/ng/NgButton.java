package io.xol.chunkstories.gui.ng;

import io.xol.chunkstories.api.gui.ClickableGuiElement;
import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class NgButton extends FocusableGuiElement implements ClickableGuiElement
{
	public String text;
	//protected BitmapFont font;
	public final Font font;
	public int size;

	protected int height;
	
	private int scale = 1;
	
	private Runnable action;
	
	public NgButton(Layer layer, int x, int y, String text)
	{
		this(layer, Client.getInstance().getGameWindow().getRenderingContext().getFontRenderer().getFont("arial", 12), x, y, text);
	}
	
	public NgButton(Layer layer, Font font, int x, int y, String text)
	{
		super(layer);
		this.font = font;
		
		this.xPosition = x;
		this.yPosition = y;
		this.text = text;
		this.height = 18;
	}

	public float getWidth()
	{
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		//int width = renderer.getFontRenderer().getFont("arial", 12).getWidth(localizedText);
		int width = font.getWidth(localizedText);
		return (width + 8) * scale;
	}

	public boolean isMouseOver(Mouse mouse)
	{
		float width = getWidth();
		return (mouse.getCursorX() >= xPosition && mouse.getCursorX() < xPosition + width && mouse.getCursorY() >= yPosition && mouse.getCursorY() <= yPosition + height * scale);
	}

	@Override
	public void render(RenderingInterface renderer) {
		float width = getWidth();
		String localizedText = Client.getInstance().getContent().localization().localize(text);
		
		Texture2D buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButton2.png");
		if (isFocused() || isMouseOver())
			buttonTexture = TexturesHandler.getTexture("./textures/gui/scalableButtonOver2.png");
			
		buttonTexture.setLinearFiltering(false);
		CorneredBoxDrawer.drawCorneredBoxTiled(xPosition + (width) / 2, yPosition + 9 * scale, width, 18 * scale, 4 * scale, buttonTexture, 32, scale);
		
		//if(scale == 1)
		renderer.getFontRenderer().drawString(renderer.getFontRenderer().getFont("arial", 12), xPosition + 4 * scale, yPosition, localizedText, scale, new Vector4fm(76/255f, 76/255f, 76/255f, 1));
		//else
		//	TrueTypeFontRenderer.get().drawString(TrueTypeFont.arial24px18pt, posx + 4 * scale, posy + 2, text, scale / 2, new Vector4fm(76/255f, 76/255f, 76/255f, 1));
	}

	@Override
	public boolean handleClick(MouseButton mouseButton) {
		if(!mouseButton.equals("mouse.left"))
			return false;

		System.out.println("hitler"+mouseButton.getName());
		
		if(this.action != null)
			this.action.run();
		
		return true;
	}

	public void setAction(Runnable runnable) {
		this.action = runnable;
	}
}
