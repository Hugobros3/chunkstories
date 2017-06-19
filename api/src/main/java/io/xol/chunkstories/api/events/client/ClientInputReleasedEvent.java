package io.xol.chunkstories.api.events.client;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.categories.ClientEvent;
import io.xol.chunkstories.api.input.Input;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Called when the client releases an input of some sort. */
public class ClientInputReleasedEvent extends Event
{
	// Every event class has to have this

	static EventListeners listeners = new EventListeners(ClientInputReleasedEvent.class);

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
	
	public ClientInputReleasedEvent(Input input)
	{
		this.input = input;
	}
	
	Input input;
	
	public Input getInput()
	{
		return input;
	}

}
