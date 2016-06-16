package io.xol.chunkstories.gui.menus;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.entity.EntityInventory;
import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.input.KeyBinds;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.InventoryDrawer;
import io.xol.chunkstories.net.packets.PacketInventoryMoveItemPile;
import io.xol.chunkstories.world.WorldRemoteClient;
import io.xol.chunkstories.world.WorldLocalClient;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.font.TrueTypeFont;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InventoryOverlay extends Overlay
{
	EntityInventory[] inventories;
	InventoryDrawer[] drawers;

	
	public static ItemPile selectedItem;
	public static int selectedItemAmount;

	public InventoryOverlay(OverlayableScene scene, Overlay parent, EntityInventory[] entityInventories)
	{
		super(scene, parent);
		this.inventories = entityInventories;
		this.drawers = new InventoryDrawer[entityInventories.length];
		for (int i = 0; i < drawers.length; i++)
			drawers[i] = new InventoryDrawer(entityInventories[i]);
	}

	@Override
	public void drawToScreen(int x, int y, int w, int h)
	{
		int totalWidth = 0;
		for (EntityInventory inv : inventories)
			totalWidth += 2 + inv.getWidth();
		totalWidth -= 2;
		int widthAccumulation = 0;
		for (int i = 0; i < drawers.length; i++)
		{
			int thisWidth = inventories[i].getWidth();
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
			
			if(selectedItemAmount > 1)
				TrueTypeFont.arial12.drawStringWithShadow(Mouse.getX() - width / 2 + (selectedItem.getItem().getSlotsWidth() - 1.0f) * slotSize , Mouse.getY() - height / 2, selectedItemAmount+"", 2, 2, new Vector4f(1,1,1,1));
				
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
						if(button == 0)
						{
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : selectedItem.getAmount();
						}
						else if(button == 1)
						{
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : 1;
						}
						else if(button == 2)
						{
							selectedItem = inventories[i].getItemPileAt(x, y);
							selectedItemAmount = selectedItem == null ? 0 : (selectedItem.getAmount() > 1 ? selectedItem.getAmount()/2 : 1);
						}
						//selectedItemInv = inventory;
					}
					else if(button == 1)
					{
						if(selectedItem.equals(inventories[i].getItemPileAt(x, y)))
						{
							if(selectedItemAmount < inventories[i].getItemPileAt(x, y).getAmount())
								selectedItemAmount++;
						}
					}
					else if(button == 0)
					{
						if(x == selectedItem.x && y == selectedItem.y)
						{
							//System.out.println("item put back into place so meh");
							selectedItem = null;
							return true;
						}
						
						if (Client.world instanceof WorldLocalClient)
						{
							//If move was successfull
							if(selectedItem.moveItemPileTo(inventories[i], x, y, selectedItemAmount))
								selectedItem = null;
						}
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
							packetMove.amount = selectedItemAmount;
							Client.connection.sendPacket(packetMove);
							//selectedItem = selectedItem.moveTo(inventories[i], x, y, selectedItemAmount);
							//if(selectedItem.moveTo(inventories[i], x, y, selectedItemAmount))
								selectedItem = null;
						}
						else
							if(selectedItem.moveItemPileTo(inventories[i], x, y, selectedItemAmount))
								selectedItem = null;
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
			packetMove.amount = selectedItemAmount;
			packetMove.itemPile = selectedItem;
			Client.connection.sendPacket(packetMove);
			selectedItem = null;//selectedItem.moveTo(inventories[i], x, y);
		}
		return true;

	}
}
