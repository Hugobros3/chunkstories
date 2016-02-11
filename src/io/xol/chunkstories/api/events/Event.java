package io.xol.chunkstories.api.events;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Event
{
	boolean allowed = true;
	
	public void setEventAllowedToExecute(boolean allowed)
	{
		this.allowed = allowed;
	}
	
	public boolean isAllowedToExecute()
	{
		return allowed;
	}
	
	public abstract EventListeners getListeners();
	
	/**
	 * Some events have a default behaviour and it's always overriding this method.
	 * Disallowing certains events to execute may cause large issues and it's not always recommanded to do so.
	 * It's more often better to just change the specific event variables to obtain the desired result.
	 */
	public void defaultBehaviour()
	{
		
	}
}
