package io.xol.chunkstories.api.events.core;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.plugin.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerLogoutEvent extends Event
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
	public String connectionMessage;
	
	public PlayerLogoutEvent(Player player)
	{
		this.player = player;
		this.connectionMessage = "#FFFF00"+player+" left the server";
	}

	public Player getPlayer()
	{
		return player;
	}
}
