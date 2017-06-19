package io.xol.chunkstories.api.events;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class CancellableEvent extends Event {
	
	private boolean canceled = false;

	public void setCancelled(boolean canceled)
	{
		this.canceled  = canceled;
	}
	
	public boolean isCancelled()
	{
		return canceled;
	}
}
