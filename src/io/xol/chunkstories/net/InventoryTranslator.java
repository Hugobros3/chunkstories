package io.xol.chunkstories.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.Inventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.net.packets.PacketsProcessor;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * Helper class defining how the game protocol should handle the inventories
 */
public class InventoryTranslator
{
	public static void writeInventoryHandle(DataOutputStream out, Inventory inventory) throws IOException
	{
		if(inventory == null || inventory.getHolder() == null)
			out.writeByte(0x00);
		else if(inventory.getHolder() instanceof Entity)
		{
			out.writeByte(0x01);
			out.writeLong(((Entity)inventory.getHolder()).getUUID());
		}
		else
			throw new RuntimeException("Untranslatable and Unknown Inventory : "+inventory+", can't describe it in outgoing packets");
	}
	
	public static Inventory obtainInventoryHandle(DataInputStream in, PacketsProcessor context) throws IOException
	{
		byte holderType = in.readByte();
		if(holderType == 0x01)
		{
			long eIdTo = in.readLong();
			
			EntityWithInventory entity = (EntityWithInventory) context.getWorld().getEntityByUUID(eIdTo);
			if(entity != null)
				return entity.getInventory();
		}
		
		return null;
	}
}
