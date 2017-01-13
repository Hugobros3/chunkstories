package io.xol.chunkstories.api.events;

import java.util.HashSet;
import java.util.Set;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EventListeners
{
	RegisteredListener[] listenersBaked;
	
	Set<RegisteredListener> unbaked = new HashSet<RegisteredListener>();
	
	public EventListeners()
	{
		bake();
	}

	public void registerListener(RegisteredListener RegisteredListener)
	{
		unbaked.add(RegisteredListener);
		bake();
	}
	
	public void unRegisterListener(RegisteredListener RegisteredListener)
	{
		unbaked.remove(RegisteredListener);
		bake();
	}
	
	private void bake()
	{
		listenersBaked = new RegisteredListener[unbaked.size()];
		//TODO weight by priority
		int i = 0;
		for(RegisteredListener l : unbaked)
		{
			listenersBaked[i] = l;
			i++;
		}
	}

	public RegisteredListener[] getListeners()
	{
		return listenersBaked;
	}

}
