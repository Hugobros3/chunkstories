package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerSelectItemEvent extends Event
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
	
	@Override
	public void defaultBehaviour()
	{
		System.out.println("Asking to select slot "+newSlot);
		//entity.getInventory().setSelectedSlot(newSlot);
	}
	
}
