package io.xol.chunkstories.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.InventoryHolder;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.util.IterableIterator;
import io.xol.chunkstories.entity.components.EntityComponentInventory;
import io.xol.chunkstories.item.inventory.InventoryLocalCreativeMenu;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Helper class defining how the game protocol should handle the inventories
 */
public class InventoryTranslator
{
	public static void writeInventoryHandle(DataOutputStream out, Inventory inventory) throws IOException
	{
		if(inventory instanceof InventoryLocalCreativeMenu)
			out.writeByte(0x02);
		else if(inventory == null || inventory.getHolder() == null)
			out.writeByte(0x00);
		else if(inventory instanceof EntityComponentInventory.EntityInventory)
		{
			EntityComponentInventory.EntityInventory entityInventory = (EntityComponentInventory.EntityInventory)inventory;
			
			out.writeByte(0x01);
			out.writeLong(((Entity)inventory.getHolder()).getUUID());
			out.writeShort(entityInventory.asComponent().getEntityComponentId());
		}
		else
			throw new RuntimeException("Untranslatable and Unknown Inventory : "+inventory+", can't describe it in outgoing packets");
	}
	
	public static Inventory obtainInventoryHandle(DataInputStream in, PacketsProcessor context) throws IOException
	{
		byte holderType = in.readByte();
		if(holderType == 0x01)
		{
			long uuid = in.readLong();
			short componentId = in.readShort();
			
			Entity entity = context.getWorld().getEntityByUUID(uuid);
			EntityComponent cpn = entity.getComponents().getComponentById(componentId);
			if(cpn != null && cpn instanceof EntityComponentInventory) {
				return ((EntityComponentInventory) cpn).getInventory();
			}
		}
		else if(holderType == 0x02)
			return INVENTORY_CREATIVE_TRASH;
		
		return null;
	}
	
	public static final InventoryLocalCreativeMenu INVENTORY_CREATIVE_TRASH = new InventoryLocalCreativeMenu() {

		@Override
		public String getInventoryName()
		{
			return "CREATIVE_TRASH";
		}

		@Override
		public ItemPile getItemPileAt(int x, int y)
		{
			return null;
		}

		@Override
		public IterableIterator<ItemPile> iterator()
		{
			throw new UnsupportedOperationException("INVENTORY_CREATIVE_TRASH.iterator()");
		}

		@Override
		public boolean isAccessibleTo(Entity entity)
		{
			return true;
		}
		
	};
}
