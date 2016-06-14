package io.xol.chunkstories.api.events.core;

import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.net.packets.PacketInventoryMoveItemPile;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerMoveItemEvent extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners();
	
	@Override
	public EventListeners getListeners()
	{
		return listeners;
	}
	
	public static EventListeners getListenersStatic()
	{
		return listeners;
	}
	
	// Specific event code
	
	public Player player;
	public PacketInventoryMoveItemPile packet;
	public ItemPile pile;
	
	public PlayerMoveItemEvent(Player player, PacketInventoryMoveItemPile packet)
	{
		this.player = player;
		this.packet = packet;

		pile = packet.itemPile;
		if(pile == null && packet.from != null)
			pile = packet.from.getItemPileAt(packet.oldX, packet.oldY);
	}

	public Player getPlayer()
	{
		return player;
	}
	
	@Override
	public void defaultBehaviour()
	{
		//System.out.println("Asking to move "+pile+" to "+packet.newX+":"+packet.newY);
		if(packet.from == null)
		{
			//player.sendMessage("Notice : dragging stuff from /dev/null to your inventory should be limited by permission.");
			if(player.hasPermission("items.spawn") || (player.getControlledEntity() != null 
					&& player.getControlledEntity() instanceof EntityCreative && ((EntityCreative) player.getControlledEntity()).getCreativeModeComponent().isCreativeMode()))
			{
				
			}
			else
			{
				player.sendMessage("#C00000You are neither in creative mode nor have the items.spawn permission.");
				return;
			}
		}
		
		if(packet.to == null)
			player.sendMessage("Notice : throwing stuff on ground is not yet implemented.");
		
		
		pile.moveTo(packet.to, packet.newX, packet.newY, packet.amount);
	}
	
}
