//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joml.Vector4f;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.Mod;
import io.xol.chunkstories.api.exceptions.content.mods.ModLoadFailureException;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.mods.ModFolder;
import io.xol.chunkstories.content.mods.ModImplementation;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.chunkstories.gui.ng.LargeButtonIcon;
import io.xol.chunkstories.gui.ng.ScrollableContainer;
import io.xol.chunkstories.gui.ng.ScrollableContainer.ContainerElement;
import io.xol.chunkstories.gui.ng.ThinButton;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay.ModsScrollableContainer.ModItem;
import io.xol.chunkstories.renderer.opengl.texture.Texture2DAsset;
import io.xol.chunkstories.renderer.opengl.texture.Texture2DGL;
import io.xol.chunkstories.renderer.opengl.texture.TexturesHandler;

public class ModsSelectionOverlay extends Layer
{
	LargeButtonIcon applyMods = new LargeButtonIcon(this, "validate");
	
	ThinButton locateExtMod = new ThinButton(this, 0, 0, ("Locate external mod"));
	ThinButton openModsFolder = new ThinButton(this, 0, 0, ("Open mods folder"));
	
	LargeButtonIcon backOption = new LargeButtonIcon(this, "back");
	
	ModsScrollableContainer modsContainer = new ModsScrollableContainer(this);
	
	public ModsSelectionOverlay(GameWindow window, Layer parent)
	{
		super(window, parent);
		
		elements.add(modsContainer);
		
		elements.add(locateExtMod);
		elements.add(openModsFolder);
		elements.add(backOption);
		elements.add(applyMods);
		
		this.backOption.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}
		});
		
		this.applyMods.setAction(new Runnable() {
			@Override
			public void run() {
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
		});
		
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
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.getRootLayer().render(renderingContext);
		
		backOption.setPosition(xPosition + 8, 8);
		backOption.render(renderingContext);

		// Display buttons
		
		float totalLengthOfButtons = 0;
		float spacing = -1;
		
		totalLengthOfButtons += applyMods.getWidth();
		totalLengthOfButtons += spacing;
		
		totalLengthOfButtons += locateExtMod.getWidth();
		totalLengthOfButtons += spacing;
		
		//totalLengthOfButtons += openModsFolder.getWidth();
		//totalLengthOfButtons += spacing;
		
		float buttonDisplayX = renderingContext.getWindow().getWidth() / 2 - totalLengthOfButtons / 2;
		float buttonDisplayY = 8;

		locateExtMod.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += locateExtMod.getWidth() + spacing;
		locateExtMod.render(renderingContext);

		openModsFolder.setPosition(buttonDisplayX, buttonDisplayY);
		buttonDisplayX += openModsFolder.getWidth() + spacing;
		openModsFolder.render(renderingContext);
		
		applyMods.setPosition(this.getWidth() - applyMods.getWidth() - 8, 8);
		buttonDisplayX += applyMods.getWidth() + spacing;
		applyMods.render(renderingContext);
		
		int s = Client.getInstance().getGameWindow().getGuiScale();
		
		modsContainer.setPosition((width - 480 * s) / 2, 32);
		modsContainer.setDimensions(480 * s, height - 32 - 32 * s);
		modsContainer.render(renderingContext);
	}
	
	@Override
	public boolean handleInput(Input input) {
		if(input instanceof MouseScroll) {
			MouseScroll ms = (MouseScroll)input;
			modsContainer.scroll(ms.amount() > 0);
			return true;
		}
		
		return super.handleInput(input);
	}
	
	class ModsScrollableContainer extends ScrollableContainer {
		
		protected ModsScrollableContainer(Layer layer) {
			super(layer);
		}

		public void render(RenderingInterface renderer)
		{
			super.render(renderer);
			
			String text = "Showing elements ";
			
			text += scroll;
			text +="->";
			text += scroll;
			
			text+=" out of "+elements.size();
			int dekal = renderer.getFontRenderer().getFont("LiberationSans-Regular", 12).getWidth(text) / 2;
			renderer.getFontRenderer().drawString(renderer.getFontRenderer().getFont("LiberationSans-Regular", 12), xPosition + width / 2 - dekal * scale(), yPosition + 16 / scale(), text, scale(), new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
			
			//return r;
		}
		
		class ModItem extends ContainerElement {

			boolean enabled;
			
			Texture2DGL icon;
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
			public boolean handleClick(MouseButton mouseButton)
			{
				Mouse mouse = mouseButton.getMouse();
				if(isOverUpButton(mouse))
				{
					int indexInList = ModsScrollableContainer.this.elements.indexOf(this);
					if(indexInList > 0)
					{
						int newIndex = indexInList - 1;
						ModsScrollableContainer.this.elements.remove(indexInList);
						ModsScrollableContainer.this.elements.add(newIndex, this);
					}
				}
				else if(isOverDownButton(mouse))
				{
					int indexInList = ModsScrollableContainer.this.elements.indexOf(this);
					if(indexInList < ModsScrollableContainer.this.elements.size() - 1)
					{
						int newIndex = indexInList + 1;
						ModsScrollableContainer.this.elements.remove(indexInList);
						ModsScrollableContainer.this.elements.add(newIndex, this);
					}
				}
				else if(isOverEnableDisableButton(mouse))
				{
					//TODO: Check for conflicts when enabling
					enabled = !enabled;
				}
				else
					return false;
				return true;
			}
			
			public boolean isOverUpButton(Mouse mouse)
			{
				int s = ModsScrollableContainer.this.scale();
				double mx = mouse.getCursorX();
				double my = mouse.getCursorY();
				
				float positionX = this.positionX + 460 * s;
				float positionY = this.positionY + 37 * s;
				int width = 18;
				int height = 17;
				return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
			}
			
			public boolean isOverEnableDisableButton(Mouse mouse)
			{
				int s = ModsScrollableContainer.this.scale();
				double mx = mouse.getCursorX();
				double my = mouse.getCursorY();

				float positionX = this.positionX + 460 * s;
				float positionY = this.positionY + 20 * s;
				int width = 18;
				int height = 17;
				return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
			}
			
			public boolean isOverDownButton(Mouse mouse)
			{
				int s = ModsScrollableContainer.this.scale();
				double mx = mouse.getCursorX();
				double my = mouse.getCursorY();

				float positionX = this.positionX + 460 * s;
				float positionY = this.positionY + 2 * s;
				int width = 18;
				int height = 17;
				return mx >= positionX && mx <= positionX + width * s && my >= positionY && my <= positionY + height * s;
			}
			
			@Override
			public void render(RenderingInterface renderer)
			{
				Mouse mouse = renderer.getClient().getInputsManager().getMouse();
				
				int s = ModsScrollableContainer.this.scale();
				//Setup textures
				Texture2DGL bgTexture = TexturesHandler.getTexture(isMouseOver(mouse) ? "./textures/gui/modsOver.png" : "./textures/gui/mods.png");
				bgTexture.setLinearFiltering(false);
				
				Texture2DGL upArrowTexture = TexturesHandler.getTexture("./textures/gui/modsArrowUp.png");
				upArrowTexture.setLinearFiltering(false);
				Texture2DGL downArrowTexture = TexturesHandler.getTexture("./textures/gui/modsArrowDown.png");
				downArrowTexture.setLinearFiltering(false);
				Texture2DGL enableDisableTexture = TexturesHandler.getTexture("./textures/gui/modsEnableDisable.png");
				enableDisableTexture.setLinearFiltering(false);
				
				//Render graphical base
				renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, bgTexture, true, false, enabled ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : new Vector4f(1.0f, 0.5f, 0.5f, 1.0f));
				//Render subbuttons
				if(isOverUpButton(mouse))
					renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, upArrowTexture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
				if(isOverEnableDisableButton(mouse))
					renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, enableDisableTexture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
				if(isOverDownButton(mouse))
					renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX, positionY, width * s, height * s, 0, 1, 1, 0, downArrowTexture, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
				
				//Render icon
				renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(positionX + 4 * s, positionY + 4 * s, 64 * s, 64 * s, 0, 1, 1, 0, icon, true, false, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
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
			
		}
	}

}
