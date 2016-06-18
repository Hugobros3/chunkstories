package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.categories.PlayerEvent;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerLoginEvent extends CancellableEvent implements PlayerEvent
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
	
	public String getConnectionMessage()
	{
		return connectionMessage;
	}

	public void setConnectionMessage(String connectionMessage)
	{
		this.connectionMessage = connectionMessage;
	}

	public String getRefusedConnectionMessage()
	{
		return refusedConnectionMessage;
	}

	public void setRefusedConnectionMessage(String refusedConnectionMessage)
	{
		this.refusedConnectionMessage = refusedConnectionMessage;
	}

	private Player player;
	private String connectionMessage;
	private String refusedConnectionMessage = "Connection was refused by a plugin.";
	
	public PlayerLoginEvent(Player player)
	{
		this.player = player;
		this.connectionMessage = "#FFFF00"+player+" joined the server";
	}
	
	@Override
	public Player getPlayer()
	{
		return player;
	}
}
