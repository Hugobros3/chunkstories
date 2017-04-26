package io.xol.chunkstories.api.events.player;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PlayerInputPressedEvent extends CancellableEvent
{
	// Every event class has to have this

	static EventListeners listeners = new EventListeners(PlayerInputPressedEvent.class);

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
	
	public PlayerInputPressedEvent(Player player, Input input)
	{
		this.player = player;
		this.input = input;
	}
	
	public Input getInput()
	{
		return input;
	}
	
	public Player getPlayer()
	{
		return player;
	}
}
