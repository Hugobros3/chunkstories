package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.events.item.EventItemDroppedToWorld;
import io.xol.chunkstories.api.events.player.PlayerMoveItemEvent;
import io.xol.chunkstories.api.exceptions.NullItemException;
import io.xol.chunkstories.api.exceptions.UndefinedItemTypeException;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.item.inventory.InventoryTranslator;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.server.ServerPacketsProcessor.ServerPlayerPacketsProcessor;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketWorld;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketInventoryMoveItemPile extends PacketWorld
{
	public ItemPile itemPile;
	public Inventory from, to;
	public int oldX, oldY, newX, newY;
	public int amount;
	
	
	public PacketInventoryMoveItemPile(World world, ItemPile itemPile, Inventory from, Inventory to, int oldX, int oldY, int newX, int newY, int amount)
	{
		super(world);
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
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext context) throws IOException
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
			itemPile.saveIntoStream(context.getWorld().getContentTranslator(), out);
		}
	}

	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException
	{
		if(!(processor instanceof ServerPlayerPacketsProcessor))
		{
			processor.logger().warn("Received a "+this.getClass().getSimpleName()+" but this GameContext isn't providen with a packet processor made to deal with it");
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
		if(from == null)// || from == InventoryTranslator.INVENTORY_CREATIVE_TRASH)
		{
			try
			{
				itemPile = ItemPile.obtainItemPileFromStream(player.getWorld().getContentTranslator(), in);
			}
			catch (NullItemException e)
			{
				//This ... isn't supposed to happen
				processor.logger().info("User "+sender+" is trying to spawn a null ItemPile for some reason.");
			}
			catch (UndefinedItemTypeException e)
			{
				//This is slightly more problematic
				processor.logger().warn(e.getMessage());
				//e.printStackTrace(processor.getLogger().getPrintWriter());
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
			if(from == null)// || from instanceof InventoryLocalCreativeMenu)
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
				
				//If we're pulling this out of an inventory ( and not /dev/null ), we need to remove it from that
				Inventory sourceInventory = itemPile.getInventory();

				Location loc = playerEntity.getLocation();
				EventItemDroppedToWorld dropItemEvent = new EventItemDroppedToWorld(loc, sourceInventory, itemPile);
				player.getContext().getPluginManager().fireEvent(dropItemEvent);
			
				if(!dropItemEvent.isCancelled()) {
					
					if(sourceInventory != null)
						sourceInventory.setItemPileAt(itemPile.getX(), itemPile.getY(), null);
					
					if(dropItemEvent.getItemEntity() != null)
						loc.getWorld().addEntity(dropItemEvent.getItemEntity());
				}
				
				return;
			}
			
			itemPile.moveItemPileTo(to, newX, newY, amount);
		}
	}
}
