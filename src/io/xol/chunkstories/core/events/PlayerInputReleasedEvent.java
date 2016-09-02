package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerInputReleasedEvent extends Event
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
	
	Player player;
	Input input;
	
	public PlayerInputReleasedEvent(Player player, Input input)
	{
		this.player = player;
		this.input = input;
	}
	
	public Player getPlayer()
	{
		return player;
	}
	
	public Input getInput()
	{
		return input;
	}

	@Override
	public void defaultBehaviour()
	{
		
	}
}
