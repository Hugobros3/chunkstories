package io.xol.chunkstories.api.events.core;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.plugin.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerSelectItemEvent extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners();
	
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
	public Entity playerEntity;
	public int newSlot;
	
	public PlayerSelectItemEvent(Player player, Entity playerEntity, int newSlot)
	{
		this.player = player;
		this.playerEntity = playerEntity;
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
		playerEntity.getInventory().setSelectedSlot(newSlot);
	}
	
}
