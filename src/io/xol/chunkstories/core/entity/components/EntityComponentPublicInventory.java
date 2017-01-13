package io.xol.chunkstories.core.entity.components;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.ItemPile;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.net.packets.PacketInventoryPartialUpdate;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentPublicInventory extends EntityComponentInventory
{
	public EntityComponentPublicInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder, width, height);
	}

	@Override
	public void refreshCompleteInventory()
	{
		if(this.entity.getWorld() instanceof WorldMaster)
			this.pushComponentEveryone();
	}
	
	public void refreshItemSlot(int x, int y, ItemPile pileChanged)
	{
		Packet packetItemUpdate = new PacketInventoryPartialUpdate(this, x, y, pileChanged);
		Controller controller = null;
		if(entity instanceof EntityControllable)
			controller = ((EntityControllable) entity).getControllerComponent().getController();
		
		if(controller != null)
			controller.pushPacket(packetItemUpdate);
		
		for(Subscriber sub : entity.getAllSubscribers())
			sub.pushPacket(packetItemUpdate);
	}
	
	@Override
	public String getHolderName()
	{
		return "Chest";
	}
}
