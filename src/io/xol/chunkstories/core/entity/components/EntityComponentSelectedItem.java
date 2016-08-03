package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemType;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.ItemsList;

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
		
		ItemPile pile = inventory.getItemPileAt(selectedSlot, 0);
		System.out.println("Sending slot"+pile);
		//don't bother writing the item pile if we're not master or if we'd be telling the controller about it
		if(pile == null || !(entity.getWorld() instanceof WorldMaster) || (destinator instanceof EntityControllable && destinator.equals(((EntityControllable) entity).getControllerComponent())))
			dos.writeBoolean(false);
		else
		{
			System.out.println("Sending item");
			dos.writeBoolean(true);
			dos.writeInt(pile.getItem().getID());
			pile.saveCSF(dos);
		}
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		selectedSlot = dis.readInt();
		
		boolean itemIncluded = dis.readBoolean();
		if(itemIncluded)
		{
			System.out.println("reading item from packet for entity"+entity);
			ItemPile pile;
			
			int id = dis.readInt() & 0x00FFFFFF;
			ItemType itemType = ItemsList.getItemTypeById(id);
			if(itemType != null)
			{
				Item item = itemType.newItem();
				pile = new ItemPile(item, dis);
				if(pile != null && !(entity.getWorld() instanceof WorldMaster))
				{
					System.out.println("got held item for "+entity + " : "+pile);
					inventory.setItemPileAt(selectedSlot, 0, pile);
				}
			}
		}
		
		this.pushComponentEveryoneButController();
	}
}
