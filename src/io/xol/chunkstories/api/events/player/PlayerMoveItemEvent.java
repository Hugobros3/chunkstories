package io.xol.chunkstories.api.events.player;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.server.Player;
import io.xol.chunkstories.net.packets.PacketInventoryMoveItemPile;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerMoveItemEvent extends CancellableEvent
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
	}

	public Player getPlayer()
	{
		return player;
	}
	
}
