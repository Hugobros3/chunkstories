package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityNameable;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.InventoryHolder;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.item.inventory.BasicInventory;
import io.xol.chunkstories.net.packets.PacketInventoryPartialUpdate;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class EntityComponentInventory extends EntityComponent
{
	protected BasicInventory actualInventory;

	//What does this inventory belong to ?
	public EntityWithInventory holder;

	//Retarded and unsafe because Java constructors are kind of dumb, for use with subtypes
	EntityComponentInventory(EntityWithInventory holder)
	{
		super(holder, holder == null ? null : holder.getComponents().getLastComponent());
		this.holder = holder;
		//this.actualInventory = ...
	}
	
	public EntityComponentInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder, holder == null ? null : holder.getComponents().getLastComponent());
		this.holder = holder;
		this.actualInventory = new EntityInventory(width, height);
	}

	public class EntityInventory extends BasicInventory
	{

		public EntityInventory(int width, int height)
		{
			super(width, height);
		}

		@Override
		public InventoryHolder getHolder()
		{
			return holder;
		}

		@Override
		public String getInventoryName()
		{
			if (holder != null)
			{
				if (holder instanceof EntityNameable)
					return ((EntityNameable) holder).getName();
				return holder.getClass().getSimpleName();
			}
			return "/dev/null";
		}

		@Override
		public void refreshItemSlot(int x, int y)
		{
			super.refreshItemSlot(x, y);
		}

		public void refreshItemSlot(int x, int y, ItemPile pileChanged)
		{
			Packet packetItemUpdate = new PacketInventoryPartialUpdate(this, x, y, pileChanged);
			Controller controller = null;
			if (entity instanceof EntityControllable)
				controller = ((EntityControllable) entity).getControllerComponent().getController();

			if (controller != null)
				controller.pushPacket(packetItemUpdate);
		}
		
		public void refreshCompleteInventory()
		{
			pushComponentController();
		}

		public EntityComponentInventory asComponent()
		{
			return EntityComponentInventory.this;
		}
		
		public boolean hasAccess(Entity entity) {
			
			if(entity == null)
				return true;
			
			//You have access to yourself always
			if(entity == EntityComponentInventory.this.entity)
				return true;
			
			//Dead entities ain't got no rights
			if(EntityComponentInventory.this.entity instanceof EntityLiving && ((EntityLiving)EntityComponentInventory.this.entity).isDead())
				return true;
			
			//Wassup with that freeloading shit ?
			return false;
		}
	}

	public enum UpdateMode
	{
		//MOVE_ITEM, 
		//CHANGE_ITEM, 
		TOTAL_REFRESH,
		NEVERMIND,
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream stream) throws IOException
	{
		//Check that person has permission
		if(destinator instanceof Player) 
		{
			Player player = (Player)destinator;
			Entity entity = player.getControlledEntity();
			
			//Abort if the entity don't have access
			if(!this.actualInventory.hasAccess(entity))
			{
				//System.out.println(player + "'s " + entity + " don't have access to "+this);
				stream.writeByte(UpdateMode.NEVERMIND.ordinal());
				return;
			}
		}
		stream.writeByte(UpdateMode.TOTAL_REFRESH.ordinal());

		actualInventory.pushInventory(destinator, stream);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream stream) throws IOException
	{
		//Unused
		byte b = stream.readByte();
		
		//Ignore NVM stuff
		if(b == UpdateMode.NEVERMIND.ordinal())
			return;

		actualInventory.pullInventory(from, stream, entity.getWorld().getGameContext().getContent());
	}

	public Inventory getInventory()
	{
		return actualInventory;
	}
}
