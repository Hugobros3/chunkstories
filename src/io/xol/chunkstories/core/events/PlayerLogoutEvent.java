package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.categories.PlayerEvent;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerLogoutEvent extends Event implements PlayerEvent
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
	
	private Player player;
	private String logoutMessage;
	
	public PlayerLogoutEvent(Player player)
	{
		this.player = player;
		this.logoutMessage = "#FFFF00"+player+" left the server";
	}

	public String getLogoutMessage()
	{
		return logoutMessage;
	}

	public void setLogoutMessage(String logoutMessage)
	{
		this.logoutMessage = logoutMessage;
	}

	@Override
	public Player getPlayer()
	{
		return player;
	}
}
