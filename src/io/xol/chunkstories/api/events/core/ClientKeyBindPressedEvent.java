package io.xol.chunkstories.api.events.core;

import io.xol.chunkstories.api.events.CancellableEvent;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.input.KeyBind;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ClientKeyBindPressedEvent extends CancellableEvent
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
	
	public ClientKeyBindPressedEvent(KeyBind keyBind)
	{
		this.keyBind = keyBind;
	}
	
	KeyBind keyBind;
	
	public KeyBind getKeyPressed()
	{
		return keyBind;
	}

}
