package io.xol.chunkstories.gui.ng;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Mouse;

import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.gui.elements.GuiElement;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ScrollableContainer extends GuiElement
{
	public List<ContainerElement> elements = new ArrayList<ContainerElement>();
	
	protected int width = 480, height = 1024;
	
	protected int scroll = 0;
	
	public void setDimensions(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	
	public void handleClick(int button, int x, int y)
	{
		int startY = this.posy + height;
		int i = scroll;
		
		while(true)
		{
			if(i >= elements.size())
				break;
			ContainerElement element = elements.get(i);
			if(startY - element.height * scale < this.posy)
				break;
			startY -= element.height * scale;
			startY -= 4 * scale;
			i++;
			
			if(element.isMouseOver())
			{
				element.clicked();
				break;
			}
		}
	}
	
	public boolean isMouseOver(int mx, int my)
	{
		return mx >= posx && mx <= posx + width && my >= posy && my <= posy + height;
	}
	
	public int render()
	{
		int r = 0;
		
		int startY = this.posy + height;
		int i = scroll;
		
		while(true)
		{
			if(i >= elements.size())
				break;
			ContainerElement element = elements.get(i);
			startY -= element.height * scale;
			if(startY - element.height * scale < this.posy)
				break;
			startY -= 4 * scale;
			i++;
			
			element.setPosition(this.posx, startY);
			element.render();
			r++;
		}
		
		return r;
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
		protected int positionX, positionY;
		protected int width = 480, height = 72;
		
		public void setPosition(int positionX, int positionY)
		{
			this.positionX = positionX;
			this.positionY = positionY;
		}
		
		public void render()
		{
			int s = ScrollableContainer.this.scale;
			//Setup textures
			Texture2D bgTexture = TexturesHandler.getTexture(isMouseOver() ? "./textures/gui/genericOver.png" : "./textures/gui/generic.png");
			bgTexture.setLinearFiltering(false);
			
			//Render graphical base
			GameWindowOpenGL.instance.renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, bgTexture, true, false, new Vector4f(1.0, 1.0, 1.0, 1.0));
			//Render icon
			GameWindowOpenGL.instance.renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX + 4 * s, positionY + 4 * s, 64 * s, 64 * s, 0, 1, 1, 0, TexturesHandler.getTexture(iconTextureLocation), true, false, new Vector4f(1.0, 1.0, 1.0, 1.0));
			//Text !
			if(name != null)
				GameWindowOpenGL.instance.renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, positionX + 70 * s, positionY + 54 * s, name, s, new Vector4f(0.0, 0.0, 0.0, 1.0));
			
			if(topRightString != null)
			{
				int dekal = width - TrueTypeFont.arial12px9pt.getWidth(topRightString) - 4;
				GameWindowOpenGL.instance.renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, positionX + dekal* s, positionY + 54 * s, topRightString, s, new Vector4f(0.25, 0.25, 0.25, 1.0));
			}
			
			if(descriptionLines != null)
				GameWindowOpenGL.instance.renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, positionX + 70 * s, positionY + 38 * s, descriptionLines, s, new Vector4f(0.25, 0.25, 0.25, 1.0));
			
		}
		
		public abstract void clicked();
		
		public boolean isMouseOver()
		{
			int s = ScrollableContainer.this.scale;
			int mx = Mouse.getX();
			int my = Mouse.getY();
			return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
		}
	}
}
