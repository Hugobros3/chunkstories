//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.ng;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.ClickableGuiElement;
import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.engine.graphics.textures.Texture2DGL;
import io.xol.engine.graphics.textures.TexturesHandler;

public class ScrollableContainer extends FocusableGuiElement implements ClickableGuiElement
{
	protected ScrollableContainer(Layer layer) {
		super(layer);
		
		this.width = 480;
		this.height = 1024;
	}

	public List<ContainerElement> elements = new ArrayList<ContainerElement>();
	
	//protected int width = 480, height = 1024;
	
	protected int scroll = 0;

	//public int scale = 1;
	
	public void setDimensions(float width, float height)
	{
		this.width = width;
		this.height = height;
	}
	
	protected int scale() {
		return layer.getGuiScale();
	}
	
	public boolean isMouseOver(int mx, int my)
	{
		return mx >= xPosition && mx <= xPosition + width && my >= yPosition && my <= yPosition + height;
	}
	
	public void render(RenderingInterface renderer)
	{	
		float startY = this.yPosition + height;
		int i = scroll;
		
		//renderer.getGuiRenderer().drawBoxWindowsSpace(xPosition, yPosition, xPosition + width, yPosition + height, 0, 0, 0, 0, null, true, false, new Vector4f(1.0f));
		
		while(true)
		{
			if(i >= elements.size())
				break;
			ContainerElement element = elements.get(i);
			startY -= element.height * scale();
			if(startY - element.height * scale() < this.yPosition)
				break;
			startY -= 4 * scale();
			i++;
			
			element.setPosition(this.xPosition, startY);
			element.render(renderer);
		}
		
		//return r;
	}
	
	public void scroll(boolean sign)
	{
		if(sign)
		{
			//Scroll up
			scroll--;
			if(scroll < 0)
				scroll = 0;
		}
		else
		{
			//Scroll up
			scroll++;
			if(scroll >= elements.size())
				scroll = elements.size() - 1;
		}
	}
	
	public abstract class ContainerElement {
		
		public ContainerElement(String name, String descriptionLines)
		{
			this.name = name;
			this.descriptionLines = descriptionLines;
		}
		
		public String name, topRightString = "";
		public String descriptionLines;
		
		public String iconTextureLocation = "./textures/gui/info.png";
		protected float positionX, positionY;
		protected float width = 480, height = 72;
		
		public void setPosition(float positionX, float positionY)
		{
			this.positionX = positionX;
			this.positionY = positionY;
		}
		
		public void render(RenderingInterface renderer)
		{
			int s = ScrollableContainer.this.scale();
			//Setup textures
			Texture2DGL bgTexture = TexturesHandler.getTexture(isMouseOver(renderer.getClient().getInputsManager().getMouse()) ? "./textures/gui/genericOver.png" : "./textures/gui/generic.png");
			bgTexture.setLinearFiltering(false);
			
			//Render graphical base
			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, bgTexture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
			//Render icon
			renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX + 4 * s, positionY + 4 * s, 64 * s, 64 * s, 0, 1, 1, 0, TexturesHandler.getTexture(iconTextureLocation), true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
			//Text !
			if(name != null)
				renderer.getFontRenderer().drawString(renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), positionX + 70 * s, positionY + 54 * s, name, s, new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
			
			if(topRightString != null)
			{
				float dekal = width - renderer.getFontRenderer().getFont("LiberationSans-Regular", 12).getWidth(topRightString) - 4;
				renderer.getFontRenderer().drawString(renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), positionX + dekal* s, positionY + 54 * s, topRightString, s, new Vector4f(0.25f, 0.25f, 0.25f, 1.0f));
			}
			
			if(descriptionLines != null)
				renderer.getFontRenderer().drawString(renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), positionX + 70 * s, positionY + 38 * s, descriptionLines, s, new Vector4f(0.25f, 0.25f, 0.25f, 1.0f));
			
		}
		
		public abstract boolean handleClick(MouseButton mouseButton);
		
		public boolean isMouseOver(Mouse mouse)
		{
			int s = ScrollableContainer.this.scale();
			double mx = mouse.getCursorX();//Mouse.getX();
			double my = mouse.getCursorY();//Mouse.getY();
			return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
		}
	}

	@Override
	public boolean handleClick(MouseButton mouseButton) {
		
		float startY = this.yPosition + height;
		int i = scroll;
		
		while(true)
		{
			if(i >= elements.size())
				break;
			ContainerElement element = elements.get(i);
			if(startY - element.height * scale() < this.yPosition)
				break;
			startY -= element.height * scale();
			startY -= 4 * scale();
			i++;
			
			if(element.isMouseOver(mouseButton.getMouse()))
			{
				element.handleClick(mouseButton);
				return true;
			}
		}
		
		return false;
	}
}
