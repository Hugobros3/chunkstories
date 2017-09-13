package io.xol.chunkstories.core.net.packets;

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
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.server.ServerPacketsProcessor.ServerPlayerPacketsProcessor;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.core.entity.EntityGroundItem;
import io.xol.chunkstories.core.item.inventory.InventoryLocalCreativeMenu;
import io.xol.chunkstories.core.item.inventory.InventoryTranslator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** TODO: I'd rather see this in common/ */
public class PacketInventoryMoveItemPile extends PacketSynchPrepared
{
	public ItemPile itemPile;
	public Inventory from, to;
	public int oldX, oldY, newX, newY;
	public int amount;
	
	public PacketInventoryMoveItemPile()
	{
		
	}
	
	public PacketInventoryMoveItemPile(ItemPile itemPile, Inventory from, Inventory to, int oldX, int oldY, int newX, int newY, int amount)
	{
		super();
		this.itemPile = itemPile;
		this.from = from;
		this.to = to;
		this.oldX = oldX;
		this.oldY = oldY;
		this.newX = newX;
		this.newY = newY;
		this.amount = amount;
	}

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
		InventoryTranslator.writeInventoryHandle(out, from);
		InventoryTranslator.writeInventoryHandle(out, to);
		
		//Describe the itemPile if we are trying to spawn an item from nowhere
		if(from == null || from.getHolder() == null)
		{
			//out.writeInt(itemPile.getItem().getID());
			itemPile.saveItemIntoStream(out);
		}
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException
	{
		if(!(processor instanceof ServerPlayerPacketsProcessor))
		{
			processor.getContext().logger().warning("Received a "+this.getClass().getSimpleName()+" but this GameContext isn't providen with a packet processor made to deal with it");
			return;
		}
		
		ServerPlayerPacketsProcessor sppc = (ServerPlayerPacketsProcessor)processor;
		Player player = sppc.getPlayer();
		EntityControllable playerEntity = player.getControlledEntity();
		
		oldX = in.readInt();
		oldY = in.readInt();
		newX = in.readInt();
		newY = in.readInt();
		
		amount = in.readInt();
		
		from = InventoryTranslator.obtainInventoryHandle(in, processor);
		to = InventoryTranslator.obtainInventoryHandle(in, processor);
		
		//If this pile is spawned from the void
		if(from == null || from == InventoryTranslator.INVENTORY_CREATIVE_TRASH)
		{
			try
			{
				itemPile = ItemPile.obtainItemPileFromStream(player.getWorld().getGameContext().getContent().items(), in);
			}
			catch (NullItemException e)
			{
				//This ... isn't supposed to happen
				processor.getContext().logger().log("User "+sender+" is trying to spawn a null ItemPile for some reason.", LogLevel.WARN);
			}
			catch (UndefinedItemTypeException e)
			{
				//This is slightly more problematic
				processor.getContext().logger().log(e.getMessage(), LogLevel.WARN);
				e.printStackTrace(processor.getContext().logger().getPrintWriter());
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
		
		//Check using event
		PlayerMoveItemEvent moveItemEvent = new PlayerMoveItemEvent(player, itemPile, from, to, oldX, oldY, newX, newY, amount);
		player.getContext().getPluginManager().fireEvent(moveItemEvent);
		
		if(!moveItemEvent.isCancelled())
		{
			//Restrict item spawning
			if(from == null || from instanceof InventoryLocalCreativeMenu)
			{
				//player.sendMessage("Notice : dragging stuff from /dev/null to your inventory should be limited by permission.");
				
				if(player.hasPermission("items.spawn") || (player.getControlledEntity() != null 
						&& player.getControlledEntity() instanceof EntityCreative && ((EntityCreative) player.getControlledEntity()).getCreativeModeComponent().get()))
				{
					//Let it happen when in creative mode or owns items.spawn perm
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
					System.out.println("Dropping items isn't possible if the player doesn't control any entity.");
					return;
				}
				
				Location loc = playerEntity.getLocation();
				EntityGroundItem entity = new EntityGroundItem(player.getContext().getContent().entities().getEntityTypeByName("groundItem"), loc.getWorld(), loc.x(), loc.y(), loc.z(), itemPile);
				loc.getWorld().addEntity(entity);
				
				player.sendMessage("Notice : throwing stuff on ground is still glitchy and experimental.");
			}
			
			itemPile.moveItemPileTo(to, newX, newY, amount);
		}
	}
}
