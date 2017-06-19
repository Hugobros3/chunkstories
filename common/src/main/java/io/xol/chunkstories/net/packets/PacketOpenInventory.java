package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.net.InventoryTranslator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketOpenInventory extends PacketSynchPrepared
{
	protected Inventory inventory;
	
	public PacketOpenInventory() {
		
	}
	
	public PacketOpenInventory(Inventory inventory) {
		this.inventory = inventory;
	}

	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		InventoryTranslator.writeInventoryHandle(out, inventory);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		//Does nothing, the actual handler is in PacketOpenInventoryClient
	}

}
