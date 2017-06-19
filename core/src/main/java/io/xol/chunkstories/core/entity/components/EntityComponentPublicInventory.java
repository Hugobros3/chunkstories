package io.xol.chunkstories.core.entity.components;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.entity.components.EntityComponentInventory;
import io.xol.chunkstories.net.packets.PacketInventoryPartialUpdate;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentPublicInventory extends EntityComponentInventory
{
	public static final float NEAR_DISTANCE = 8f;
	
	public EntityComponentPublicInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder);
		this.actualInventory = new EntityPublicInventory(width, height);
	}

	public class EntityPublicInventory extends EntityInventory
	{
		public EntityPublicInventory(int width, int height)
		{
			super(width, height);
		}

		@Override
		public void refreshCompleteInventory()
		{
			if (entity.getWorld() instanceof WorldMaster)
				pushComponentEveryone();
		}

		public void refreshItemSlot(int x, int y, ItemPile pileChanged)
		{
			Packet packetItemUpdate = new PacketInventoryPartialUpdate(this, x, y, pileChanged);
			Controller controller = null;
			if (entity instanceof EntityControllable)
				controller = ((EntityControllable) entity).getControllerComponent().getController();

			if (controller != null)
				controller.pushPacket(packetItemUpdate);

			for (Subscriber sub : entity.getAllSubscribers())
				sub.pushPacket(packetItemUpdate);
		}

		@Override
		public String getInventoryName()
		{
			return "Chest";
		}
		
		public boolean isAccessibleTo(Entity entity) {
			if(super.isAccessibleTo(entity))
				return true;
			
			//It's public if you're near enough.
			if(entity != null && entity.getLocation().distanceTo(EntityComponentPublicInventory.this.entity.getLocation()) <= NEAR_DISTANCE) {
				return true;
			}
			
			return false;
		}

	}
}
