package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.renderer.Camera;

public class CameraSetupEvent extends Event
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
	
	private Camera camera;
	
	public CameraSetupEvent(Camera camera)
	{
		this.camera = camera;
	}
	
	public Camera getCamera()
	{
		return camera;
	}
}
