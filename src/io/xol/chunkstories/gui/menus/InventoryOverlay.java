package io.xol.chunkstories.gui.menus;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.input.KeyBinds;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.inventory.Inventory;
import io.xol.chunkstories.item.renderer.InventoryDrawer;
import io.xol.chunkstories.net.packets.PacketInventoryMoveItemPile;
import io.xol.chunkstories.world.WorldRemoteClient;
import io.xol.chunkstories.world.WorldLocalClient;
import io.xol.engine.base.XolioWindow;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InventoryOverlay extends Overlay
{
	Inventory[] inventories;
	InventoryDrawer[] drawers;

	public static ItemPile selectedItem;

	public InventoryOverlay(OverlayableScene scene, Overlay parent, Inventory[] inventories)
	{
		super(scene, parent);
		this.inventories = inventories;
		this.drawers = new InventoryDrawer[inventories.length];
		for (int i = 0; i < drawers.length; i++)
			drawers[i] = new InventoryDrawer(inventories[i]);
	}

	@Override
	public void drawToScreen(int x, int y, int w, int h)
	{
		int totalWidth = 0;
		for (Inventory inv : inventories)
			totalWidth += 2 + inv.width;
		totalWidth -= 2;
		int widthAccumulation = 0;
		for (int i = 0; i < drawers.length; i++)
		{
			int thisWidth = inventories[i].width;
			drawers[i].drawInventoryCentered(mainScene.eng.renderingContext, XolioWindow.frameW / 2 - totalWidth * 24 + thisWidth * 24 + widthAccumulation * 48, XolioWindow.frameH / 2, 2, false, 4 - i*4);
			widthAccumulation += 1 + thisWidth;
		}

		if (selectedItem != null)
		{
			int slotSize = 24 * 2;
			/*int textureId = TexturesHandler.getTextureID(selectedItem.getTextureName());
			if(textureId == -1)
				textureId = TexturesHandler.getTexture("res/items/icons/notex.png").getID();*/
			int width = slotSize * selectedItem.item.getSlotsWidth();
			int height = slotSize * selectedItem.item.getSlotsHeight();
			//GuiDrawer.drawBoxWindowsSpaceWithSize(Mouse.getX() - width / 2, Mouse.getY() - height / 2, width, height, 0, 1, 1, 0, textureId, true, true, null);
			
			//
			selectedItem.getItem().getItemRenderer().renderItemInInventory(mainScene.eng.renderingContext, selectedItem, Mouse.getX() - width / 2, Mouse.getY() - height / 2, 2);
		}
		//System.out.println(inventories[0]);
	}

	@Override
	public boolean handleKeypress(int k)
	{
		if (KeyBinds.getKeyBind("exit").isPressed())
			this.mainScene.changeOverlay(parent);
		return true;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		for (int i = 0; i < drawers.length; i++)
		{
			if (drawers[i].isOverCloseButton())
				this.mainScene.changeOverlay(parent);
			else
			{

				int[] c = drawers[i].getSelectedSlot();
				if (c == null)
					continue;
				else
				{
					int x = c[0];
					int y = c[1];
					if (selectedItem == null)
					{
						selectedItem = inventories[i].getItem(x, y);
						//selectedItemInv = inventory;
					}
					else
					{
						if (Client.world instanceof WorldLocalClient)
							selectedItem = selectedItem.moveTo(inventories[i], x, y);
						else if(Client.world instanceof WorldRemoteClient)
						{
							PacketInventoryMoveItemPile packetMove = new PacketInventoryMoveItemPile(true);
							packetMove.from = selectedItem.inventory;
							packetMove.oldX = selectedItem.x;
							packetMove.oldY = selectedItem.y;
							packetMove.to = inventories[i];
							packetMove.newX = x;
							packetMove.newY = y;
							packetMove.itemPile = selectedItem;
							Client.connection.sendPacket(packetMove);
							selectedItem = selectedItem.moveTo(inventories[i], x, y);
						}
						else
							selectedItem = selectedItem.moveTo(inventories[i], x, y);
					}
					return true;
				}
			}
		}
		if(selectedItem != null && Client.world instanceof WorldRemoteClient)
		{
			PacketInventoryMoveItemPile packetMove = new PacketInventoryMoveItemPile(true);
			packetMove.from = selectedItem.inventory;
			packetMove.oldX = selectedItem.x;
			packetMove.oldY = selectedItem.y;
			packetMove.to = null;
			packetMove.newX = 0;
			packetMove.newY = 0;
			packetMove.itemPile = selectedItem;
			Client.connection.sendPacket(packetMove);
			selectedItem = null;//selectedItem.moveTo(inventories[i], x, y);
		}
		return true;

	}
}
