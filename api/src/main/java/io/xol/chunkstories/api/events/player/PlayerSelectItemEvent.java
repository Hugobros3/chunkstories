package io.xol.chunkstories.api.events.player;

import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.player.Player;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerSelectItemEvent extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners(PlayerSelectItemEvent.class);
	
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
	public EntityWithInventory entity;
	public int newSlot;
	
	public PlayerSelectItemEvent(Player player, EntityWithInventory playerEntity, int newSlot)
	{
		this.player = player;
		this.entity = playerEntity;
		this.newSlot = newSlot;
	}

	public Player getPlayer()
	{
		return player;
	}
	
}
