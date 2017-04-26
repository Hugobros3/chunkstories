package io.xol.chunkstories.api.events.rendering;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.rendering.CameraInterface;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class CameraSetupEvent extends Event
{
	// Every event class has to have this
	
	static EventListeners listeners = new EventListeners(CameraSetupEvent.class);
	
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
	
	private CameraInterface camera;
	
	public CameraSetupEvent(CameraInterface camera)
	{
		this.camera = camera;
	}
	
	public CameraInterface getCamera()
	{
		return camera;
	}
}
