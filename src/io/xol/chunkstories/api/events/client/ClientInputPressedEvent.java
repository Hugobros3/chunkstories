package io.xol.chunkstories.api.events.client;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.events.categories.ClientEvent;
import io.xol.chunkstories.api.input.Input;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientInputPressedEvent extends CancellableEvent implements ClientEvent
{
	// Every event class has to have this

	static EventListeners listeners = new EventListeners(ClientInputPressedEvent.class);

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

	public ClientInputPressedEvent(Input input)
	{
		this.input = input;
	}

	Input input;

	public Input getInput()
	{
		return input;
	}
}
