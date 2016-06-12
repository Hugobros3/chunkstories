package io.xol.chunkstories.entity.core.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.net.StreamTarget;
import io.xol.chunkstories.item.ItemPile;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentSelectedItem extends EntityComponent
{
	EntityComponentInventory inventory;
	
	public EntityComponentSelectedItem(Entity entity, EntityComponentInventory inventory)
	{
		super(entity, inventory.getLastComponent());
		this.inventory = inventory;
	}

	int selectedSlot = 0;

	/**
	 * Selects the slot given
	 * 
	 * @param newSlot
	 */
	public void setSelectedSlot(int newSlot)
	{
		
		
		while (newSlot < 0)
			newSlot += inventory.width;
		selectedSlot = newSlot % inventory.width;

		//TODO permissions check
		this.pushComponentEveryone();

		/*if(this.holder != null && this.holder instanceof Entity && this.holder instanceof EntityControllable && ((EntityControllable)this.holder).getController() != null
				&& ((EntityControllable)this.holder).getController() instanceof ClientController)
		{
			PacketItemUsage packet = new PacketItemUsage(true);
			packet.usage = ItemUsage.SELECT;
			packet.complementInfo = (byte) newSlot;
			if(((Entity) this.holder).getWorld() instanceof WorldRemoteClient)
				Client.connection.sendPacket(packet);
			//((ClientController)((EntityControllable)this.holder).getController()).notifySelectedItemChange();
		}*/
	}

	/**
	 * Returns the selected slot
	 * 
	 * @return
	 */
	public int getSelectedSlot()
	{
		return selectedSlot;
	}

	/**
	 * Returns the selected item
	 * 
	 * @return
	 */
	public ItemPile getSelectedItem()
	{
		return inventory.contents[selectedSlot][0];
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeInt(selectedSlot);
	}

	@Override
	protected void pull(DataInputStream dis) throws IOException
	{
		selectedSlot = dis.readInt();
	}
}
