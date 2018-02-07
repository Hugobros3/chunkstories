package io.xol.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.item.inventory.InventoryTranslator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.net.packets.PacketOpenInventory;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketOpenInventoryClient extends PacketOpenInventory
{
	public PacketOpenInventoryClient(World world) {
		super(world);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException, PacketProcessingException
	{
		inventory = InventoryTranslator.obtainInventoryHandle(in, processor);
		
		Entity entity = Client.getInstance().getPlayer().getControlledEntity();
		
		if(entity != null && entity instanceof EntityWithInventory)
			Client.getInstance().openInventories(((EntityWithInventory) entity).getInventory(), inventory);
		else
			Client.getInstance().openInventories(inventory);
	}

}
