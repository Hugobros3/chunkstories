package io.xol.chunkstories.gui.menus;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.inventory.Inventory;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.core.ItemAk47;
import io.xol.chunkstories.item.core.ItemAk47Magazine;
import io.xol.chunkstories.item.core.ItemHeGrenade;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.gui.GuiDrawer;
import io.xol.engine.textures.TexturesHandler;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InventoryOverlay extends MenuOverlay
{
	Inventory inventory;
	InventoryDrawer drawer;

	public static ItemPile selectedItem;
	public static Inventory selectedItemInv;
	
	public InventoryOverlay(OverlayableScene scene, MenuOverlay parent, Inventory inventory)
	{
		super(scene, parent);
		this.inventory = inventory;
		drawer = new InventoryDrawer(inventory);
	}
	
	public void drawToScreen(int x, int y, int w, int h)
	{
		drawer.drawInventoryCentered(XolioWindow.frameW/2, XolioWindow.frameH/2, 2, false, 4);
		
		if(selectedItem != null)
		{
			int slotSize = 24 * 2;
			int textureId = TexturesHandler.getTextureID(selectedItem.getTextureName());
			int width = slotSize * selectedItem.item.slotsWidth;
			int height = slotSize * selectedItem.item.slotsHeight;
			GuiDrawer.drawBoxWindowsSpaceWithSize(Mouse.getX()-width/2, Mouse.getY()-height/2, width, height, 0, 1, 1, 0, textureId, true, true, null);
		}
	}

	public boolean handleKeypress(int k)
	{
		if(k == FastConfig.EXIT_KEY)
			this.mainScene.changeOverlay(parent);
		return false;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if(drawer.isOverCloseButton())
			this.mainScene.changeOverlay(parent);
		else
		{
			int[] c = drawer.getSelectedSlot();
			if(c == null)
				return false;
			else
			{
				int x = c[0];
				int y = c[1];
				if(button == 2)
				{
					inventory.setItemAt(x, y, new ItemPile(new ItemHeGrenade()));
				}
				else if(button == 1)
				{
					if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
						inventory.setItemAt(x, y, new ItemPile(new ItemAk47()));
					else
						inventory.setItemAt(x, y, new ItemPile(new ItemAk47Magazine()));
				}
				else
				{
					if(selectedItem == null)
					{
						selectedItem = inventory.getItem(x, y);
						selectedItemInv = inventory;
					}
					else
					{
						inventory.setItemAt(selectedItem.x, selectedItem.y, null);
						if(inventory.canPlaceItemAt(x, y, selectedItem))
						{
							ItemPile nextSelection = inventory.getItem(x, y);
							inventory.setItemAt(x, y, selectedItem);
							selectedItem = nextSelection;
							selectedItemInv = inventory;
						}
						else
							inventory.setItemAt(selectedItem.x, selectedItem.y, selectedItem);
					}
				}
			}
		}
		return false;
	}
}
