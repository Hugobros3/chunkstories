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
}
