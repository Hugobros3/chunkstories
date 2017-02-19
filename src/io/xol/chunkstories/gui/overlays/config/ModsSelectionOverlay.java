package io.xol.chunkstories.gui.overlays.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.mods.Asset;
import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.mods.ModImplementation;
import io.xol.chunkstories.content.mods.ModFolder;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.ng.NgButton;
import io.xol.chunkstories.gui.ng.ScrollableContainer;
import io.xol.chunkstories.gui.ng.ScrollableContainer.ContainerElement;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay.ModsScrollableContainer.ModItem;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.Texture2DAsset;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
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
		
		buildModsList();
	}

	private void buildModsList()
	{
		modsContainer.elements.clear();
		Collection<String> currentlyEnabledMods = Arrays.asList(Client.getInstance().getContent().modsManager().getEnabledModsString());
		
		Set<String> uniqueMods = new HashSet<String>();
		//First put in already loaded mods
		for(Mod mod : Client.getInstance().getContent().modsManager().getCurrentlyLoadedMods())
		{
			//Should use md5 hash instead ;)
			if(uniqueMods.add(mod.getModInfo().getName().toLowerCase()))
				modsContainer.elements.add(modsContainer.new ModItem(mod, true));
		}
		
		//Then look for mods in folder fashion
		for(File f : new File(GameDirectory.getGameFolderPath()+"/mods/").listFiles())
		{
			if(f.isDirectory())
			{
				File txt = new File(f.getAbsolutePath()+"/mod.txt");
				if(txt.exists())
				{
					try
					{
						ModFolder mod = new ModFolder(f);
						//Should use md5 hash instead ;)
						if(uniqueMods.add(mod.getModInfo().getName().toLowerCase()))
							modsContainer.elements.add(modsContainer.new ModItem(mod, currentlyEnabledMods.contains(mod.getModInfo().getName())));
						
						System.out.println("mod:"+mod.getModInfo().getName() + " // " + currentlyEnabledMods.contains(mod.getModInfo().getName()));
					}
					catch (ModLoadFailureException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		//Then look for .zips
		//First look for mods in folder fashion
		for(File f : new File(GameDirectory.getGameFolderPath()+"/mods/").listFiles())
		{
			if(f.getName().endsWith(".zip"))
			{
				try
				{
					ModZip mod = new ModZip(f);
					//Should use md5 hash instead ;)
					if(uniqueMods.add(mod.getModInfo().getName().toLowerCase()))
						modsContainer.elements.add(modsContainer.new ModItem(mod, currentlyEnabledMods.contains(mod.getModInfo().getName())));
				}
				catch (ModLoadFailureException e)
				{
					e.printStackTrace();
				}
			}
		}
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
		
		float buttonDisplayX = renderingContext.getWindow().getWidth() / 2 - totalLengthOfButtons / 2;
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
		
		if(applyMods.clicked())
		{
			List<String> modsEnabled = new ArrayList<String>();
			for(ContainerElement e : modsContainer.elements)
			{
				ModItem modItem = (ModItem)e;
				if(modItem.enabled)
				{
					System.out.println("Adding "+((ModImplementation) modItem.mod).getLoadString()+" to mod path");
					modsEnabled.add(((ModImplementation) modItem.mod).getLoadString());
				}
			}
			
			String[] ok = new String[modsEnabled.size()];
			modsEnabled.toArray(ok);
			Client.getInstance().getContent().modsManager().setEnabledMods(ok);
			/*ModsManager.reload();
			ModsManager.reloadClientContent();*/
			Client.getInstance().reloadAssets();
			buildModsList();
		}
		
		int s = Client.getInstance().getGameWindow().getScalingFactor();
		
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
			GameWindowOpenGL.getInstance().renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, posx + width / 2 - dekal * scale, posy + 16 / scale, text, scale, new Vector4fm(0.0, 0.0, 0.0, 1.0));
			
			return r;
		}
		
		class ModItem extends ContainerElement {

			boolean enabled;
			
			Texture2D icon;
			Mod mod;
			
			public ModItem(Mod mod2, boolean enabled)
			{
				super(mod2.getModInfo().getName(), mod2.getModInfo().getDescription());
				this.mod = mod2;
				this.enabled = enabled;
				this.topRightString = mod2.getModInfo().getVersion();
				
				Asset asset = mod2.getAssetByName("./modicon.png");
				if(asset != null)
					icon = new Texture2DAsset(asset);
				else
					icon = TexturesHandler.getTexture("./nomodicon.png");
			}

			@Override
			public void clicked()
			{
				if(isOverUpButton())
				{
					int indexInList = ModsScrollableContainer.this.elements.indexOf(this);
					if(indexInList > 0)
					{
						int newIndex = indexInList - 1;
						ModsScrollableContainer.this.elements.remove(indexInList);
						ModsScrollableContainer.this.elements.add(newIndex, this);
					}
				}
				else if(isOverDownButton())
				{
					int indexInList = ModsScrollableContainer.this.elements.indexOf(this);
					if(indexInList < ModsScrollableContainer.this.elements.size() - 1)
					{
						int newIndex = indexInList + 1;
						ModsScrollableContainer.this.elements.remove(indexInList);
						ModsScrollableContainer.this.elements.add(newIndex, this);
					}
				}
				else if(isOverEnableDisableButton())
				{
					//TODO: Check for conflicts when enabling
					enabled = !enabled;
				}
			}
			
			public boolean isOverUpButton()
			{
				int s = ModsScrollableContainer.this.scale;
				int mx = Mouse.getX();
				int my = Mouse.getY();
				
				int positionX = this.positionX + 460 * s;
				int positionY = this.positionY + 37 * s;
				int width = 18;
				int height = 17;
				return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
			}
			
			public boolean isOverEnableDisableButton()
			{
				int s = ModsScrollableContainer.this.scale;
				int mx = Mouse.getX();
				int my = Mouse.getY();

				int positionX = this.positionX + 460 * s;
				int positionY = this.positionY + 20 * s;
				int width = 18;
				int height = 17;
				return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
			}
			
			public boolean isOverDownButton()
			{
				int s = ModsScrollableContainer.this.scale;
				int mx = Mouse.getX();
				int my = Mouse.getY();

				int positionX = this.positionX + 460 * s;
				int positionY = this.positionY + 2 * s;
				int width = 18;
				int height = 17;
				return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
			}
			
			@Override
			public void render()
			{
				int s = ModsScrollableContainer.this.scale;
				//Setup textures
				Texture2D bgTexture = TexturesHandler.getTexture(isMouseOver() ? "./textures/gui/modsOver.png" : "./textures/gui/mods.png");
				bgTexture.setLinearFiltering(false);
				
				Texture2D upArrowTexture = TexturesHandler.getTexture("./textures/gui/modsArrowUp.png");
				upArrowTexture.setLinearFiltering(false);
				Texture2D downArrowTexture = TexturesHandler.getTexture("./textures/gui/modsArrowDown.png");
				downArrowTexture.setLinearFiltering(false);
				Texture2D enableDisableTexture = TexturesHandler.getTexture("./textures/gui/modsEnableDisable.png");
				enableDisableTexture.setLinearFiltering(false);
				
				//Render graphical base
				GameWindowOpenGL.getInstance().renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, bgTexture, true, false, enabled ? new Vector4fm(1.0, 1.0, 1.0, 1.0) : new Vector4fm(1.0, 0.5, 0.5, 1.0));
				//Render subbuttons
				if(isOverUpButton())
					GameWindowOpenGL.getInstance().renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, upArrowTexture, true, false, new Vector4fm(1.0, 1.0, 1.0, 1.0));
				if(isOverEnableDisableButton())
					GameWindowOpenGL.getInstance().renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, enableDisableTexture, true, false, new Vector4fm(1.0, 1.0, 1.0, 1.0));
				if(isOverDownButton())
					GameWindowOpenGL.getInstance().renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, downArrowTexture, true, false, new Vector4fm(1.0, 1.0, 1.0, 1.0));
				
				//Render icon
				GameWindowOpenGL.getInstance().renderingContext.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX + 4 * s, positionY + 4 * s, 64 * s, 64 * s, 0, 1, 1, 0, icon, true, false, new Vector4fm(1.0, 1.0, 1.0, 1.0));
				//Text !
				if(name != null)
					GameWindowOpenGL.getInstance().renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, positionX + 70 * s, positionY + 54 * s, name, s, new Vector4fm(0.0, 0.0, 0.0, 1.0));
				
				if(topRightString != null)
				{
					int dekal = width - TrueTypeFont.arial12px9pt.getWidth(topRightString) - 4;
					GameWindowOpenGL.getInstance().renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, positionX + dekal* s, positionY + 54 * s, topRightString, s, new Vector4fm(0.25, 0.25, 0.25, 1.0));
				}
				
				if(descriptionLines != null)
					GameWindowOpenGL.getInstance().renderingContext.getTrueTypeFontRenderer().drawString(TrueTypeFont.arial12px9pt, positionX + 70 * s, positionY + 38 * s, descriptionLines, s, new Vector4fm(0.25, 0.25, 0.25, 1.0));
				
			}
			
		}
	}

}
