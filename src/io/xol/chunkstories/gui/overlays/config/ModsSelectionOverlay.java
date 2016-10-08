package io.xol.chunkstories.gui.overlays.config;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.ng.NgButton;
import io.xol.chunkstories.gui.ng.ScrollableContainer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ModsSelectionOverlay extends Overlay
{
	NgButton applyMods = new NgButton(0, 0, ("Apply Mods"));
	
	NgButton locateExtMod = new NgButton(0, 0, ("Locate external mod"));
	NgButton openModsFolder = new NgButton(0, 0, ("Open mods folder"));
	
	NgButton backOption = new NgButton(0, 0, ("Back"));
	
	ModsScrollableContainer modsContainer = new ModsScrollableContainer();
	
	public ModsSelectionOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(applyMods);
		guiHandler.add(locateExtMod);
		guiHandler.add(openModsFolder);
		
		guiHandler.add(backOption);
		
		guiHandler.add(modsContainer);
		
		modsContainer.elements.add(modsContainer.new ModItem("Mod blah", "Blah, blah\nBlah"));
		modsContainer.elements.add(modsContainer.new ModItem("Mod 1", "Blah, blah\nBlah\nBlerh"));
		modsContainer.elements.add(modsContainer.new ModItem("Mod 2", "Blah, blah\nBlah"));
		modsContainer.elements.add(modsContainer.new ModItem("Mod 3", "Blah, blah\nBlah"));
		modsContainer.elements.add(modsContainer.new ModItem("Mod 4", "Blah, blah\nBlah"));
		modsContainer.elements.add(modsContainer.new ModItem("Mod 5", "Blah, blah\nBlah"));
		modsContainer.elements.add(modsContainer.new ModItem("Mod 6", "Blah, blah\nBlah"));
		modsContainer.elements.add(modsContainer.new ModItem("Mod bla7h", "Blah, blah\nBlah"));
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int positionStartX, int positionStartY, int width, int height)
	{
		backOption.setPosition(positionStartX + 8, 8);
		backOption.draw();

		// Display buttons
		
		float totalLengthOfButtons = 0;
		float spacing = -1;
		
		totalLengthOfButtons += applyMods.getWidth();
		totalLengthOfButtons += spacing;
		
		totalLengthOfButtons += locateExtMod.getWidth();
		totalLengthOfButtons += spacing;
		
		totalLengthOfButtons += openModsFolder.getWidth();
		totalLengthOfButtons += spacing;
		
		float buttonDisplayX = GameWindowOpenGL.windowWidth / 2 - totalLengthOfButtons / 2;
		float buttonDisplayY = 4;

		locateExtMod.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += locateExtMod.getWidth() + spacing;
		locateExtMod.draw();

		openModsFolder.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += openModsFolder.getWidth() + spacing;
		openModsFolder.draw();
		
		applyMods.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += applyMods.getWidth() + spacing;
		applyMods.draw();
		
		
		if (backOption.clicked())
			this.mainScene.changeOverlay(this.parent);
		
		int s = GameWindowOpenGL.getScalingFactor();
		
		modsContainer.setPosition((width - 480 * s) / 2, 32);
		modsContainer.setDimensions(480 * s, height - 32 - 32 * s);
		modsContainer.render();
	}
	
	public boolean onScroll(int dy)
	{
		modsContainer.scroll(dy > 0);
		return true;
	}
	
	class ModsScrollableContainer extends ScrollableContainer {
		
		public int render()
		{
			int r = super.render();
			
			String text = "Showing elements ";
			
			text += scroll;
			text +="->";
			text += scroll + r;
			
			text+=" out of "+elements.size();
			int dekal = TrueTypeFont.arial12px9pt.getWidth(text) / 2;
			GameWindowOpenGL.instance.renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, posx + width / 2 - dekal * scale, posy + 16 / scale, text, scale, new Vector4f(0.0, 0.0, 0.0, 1.0));
			
			return r;
		}
		
		class ModItem extends ContainerElement {

			public ModItem(String name, String descriptionLines)
			{
				super(name, descriptionLines);
				this.topRightString = "1.0.0";
			}

			@Override
			public void clicked()
			{
				System.out.println("Kboom ! + "+name);
			}
			
			@Override
			public void render()
			{
				int s = ModsScrollableContainer.this.scale;
				//Setup textures
				Texture2D bgTexture = TexturesHandler.getTexture(isMouseOver() ? "./textures/gui/modsOver.png" : "./textures/gui/mods.png");
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
			
		}
	}

}
