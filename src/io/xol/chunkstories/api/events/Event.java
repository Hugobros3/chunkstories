package io.xol.chunkstories.api.events;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Event
{
	public abstract EventListeners getListeners();
	
	/**
	 * Executed when the event has been passed to all listening plugins.
	 * May check if event was canceled if the implementation allows it
	 */
	public abstract void defaultBehaviour();
}
