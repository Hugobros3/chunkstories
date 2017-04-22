package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.events.player.PlayerMoveItemEvent;
import io.xol.chunkstories.api.exceptions.NullItemException;
import io.xol.chunkstories.api.exceptions.UndefinedItemTypeException;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.core.entity.EntityGroundItem;
import io.xol.chunkstories.net.InventoryTranslator;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.world.WorldImplementation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketInventoryMoveItemPile extends PacketSynchPrepared
{
	public ItemPile itemPile;
	public Inventory from, to;
	public int oldX, oldY, newX, newY;
	public int amount;
	
	@Override
	public void sendIntoBuffer(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//Describe the move
		out.writeInt(oldX);
		out.writeInt(oldY);
		out.writeInt(newX);
		out.writeInt(newY);
		
		out.writeInt(amount);
		
		//Describe the inventories
		//A lone itemPile or a holderless inventory is described by 0x00
		
		InventoryTranslator.writeInventoryHandle(out, from);
		InventoryTranslator.writeInventoryHandle(out, to);
		
		/*if(from == null || from.getHolder() == null)
			out.writeByte(0x00);
		else if(from.getHolder() instanceof Entity)
		{
			out.writeByte(0x01);
			out.writeLong(((Entity)from.getHolder()).getUUID());
		}
		if(to == null || to.getHolder() == null)
			out.writeByte(0x00);
		else if(to.getHolder() instanceof Entity)
		{
			out.writeByte(0x01);
			out.writeLong(((Entity)to.getHolder()).getUUID());
			//System.out.println("writing uuid"+((Entity)to.holder).getUUID());
		}*/
		
		//Describe the itemPile if we are trying to spawn an item from nowhere
		if(from == null || from.getHolder() == null)
		{
			//out.writeInt(itemPile.getItem().getID());
			itemPile.saveItemIntoStream(out);
		}
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		Player player = processor.getServerClient().getProfile();
		EntityControllable playerEntity = player.getControlledEntity();
		
		oldX = in.readInt();
		oldY = in.readInt();
		newX = in.readInt();
		newY = in.readInt();
		
		amount = in.readInt();
		
		from = InventoryTranslator.obtainInventoryHandle(in, processor);
		to = InventoryTranslator.obtainInventoryHandle(in, processor);
		
		//If this pile is spawned from the void
		if(from == null)
		{
			try
			{
				itemPile = ItemPile.obtainItemPileFromStream(player.getWorld().getGameContext().getContent().items(), in);
			}
			catch (NullItemException e)
			{
				//This ... isn't supposed to happen
				ChunkStoriesLogger.getInstance().log("User "+sender+" is trying to spawn a null ItemPile for some reason.", LogLevel.WARN);
			}
			catch (UndefinedItemTypeException e)
			{
				//This is slightly more problematic
				ChunkStoriesLogger.getInstance().log(e.getMessage(), LogLevel.WARN);
				e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
			}
		}
		else
		{
			itemPile = from.getItemPileAt(oldX, oldY);
		}
		
		//Check access
		if(to != null && playerEntity != null)
		{
			if(!to.isAccessibleTo(playerEntity))
			{
				player.sendMessage("You don't have access to this.");
				return;
			}
		}
		
		PlayerMoveItemEvent moveItemEvent = new PlayerMoveItemEvent(player, this);
		player.getServer().getPluginManager().fireEvent(moveItemEvent);
		
		if(!moveItemEvent.isCancelled())
		{
			//System.out.println("Asking to move "+pile+" to "+packet.newX+":"+packet.newY);
			if(from == null)
			{
				//player.sendMessage("Notice : dragging stuff from /dev/null to your inventory should be limited by permission.");
				if(player.hasPermission("items.spawn") || (player.getControlledEntity() != null 
						&& player.getControlledEntity() instanceof EntityCreative && ((EntityCreative) player.getControlledEntity()).getCreativeModeComponent().get()))
				{
					
				}
				else
				{
					player.sendMessage("#C00000You are neither in creative mode nor have the items.spawn permission.");
					return;
				}
			}
			
			//If target inventory is null, this means the item was dropped
			if(to == null)
			{
				//TODO this really needs some kind of permissions system
				//TODO or not ? Maybe the cancellable event deal can prevent this
				
				if(playerEntity == null)
				{
					System.out.println("fuck off");
					return;
				}
				
				Location loc = playerEntity.getLocation();
				EntityGroundItem entity = new EntityGroundItem((WorldImplementation) loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), itemPile);
				loc.getWorld().addEntity(entity);
				
				player.sendMessage("Notice : throwing stuff on ground is still glitchy and experimental.");
			}
			
			itemPile.moveItemPileTo(to, newX, newY, amount);
		}
	}
}
