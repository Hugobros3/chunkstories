package io.xol.chunkstories.api.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import io.xol.chunkstories.api.events.voxel.VoxelEvent;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EventListeners
{
	RegisteredListener[] listenersBaked;
	Set<RegisteredListener> unbaked = new HashSet<RegisteredListener>();
	
	Set<EventListeners> children = new HashSet<EventListeners>();
	EventListeners[] childrenBaked;
	
	final String owningEventName;
	
	public EventListeners()
	{
		owningEventName = "Anonymous event";
		bake();
		bakeChildren();
	}
	
	public EventListeners(Class<? extends Event> eventClass)
	{
		owningEventName = eventClass.getName();
		System.out.println("Building EventListener for "+eventClass);
		Class<? extends Event> son = eventClass;
		//EventListeners sonListener = this;
		
		try {
			while(true) {
				Class<?> dad = son.getSuperclass();
				if(Event.class.isAssignableFrom(dad))
				{
					//Security
					if(Event.class == dad || CancellableEvent.class == dad)
						break;
					
					System.out.println("Found superclass " + dad);
					
					try {
						Method m = dad.getMethod("getListenersStatic");
					
						Object o = m.invoke(null);
						EventListeners daddyEars = (EventListeners)o;
						
						//Notice me daddy
						daddyEars.declareChildren(this);
						
						//Oedipe time
						son = (Class<? extends Event>) dad;
					}
					catch(NullPointerException npe) {
						System.out.println("Stopping inheritance lookup; stepped on NPE");
						npe.printStackTrace();
						break;
					}
					catch(NoSuchMethodException nsme) {
						System.out.println("Stopping inheritance lookup; stepped on NSME");
						nsme.printStackTrace();
						break;
					}
				}
				else
				{
					break;
				}
			}
		}
		catch(IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
			throw new RuntimeException("Error while setting up events inheritance");
		}
		
		bake();
		bakeChildren();
	}
	
	void declareChildren(EventListeners heyDad) {
		children.add(heyDad);
		
		//Debug thingie
		System.out.print("EventListener for "+ owningEventName + ", childrens = ");
		for(EventListeners l : children)
		{
			System.out.print(l.owningEventName + ", ");
		}
		System.out.println();
		
		bakeChildren();
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
	
	private void bakeChildren()
	{
		childrenBaked = new EventListeners[children.size()];
		int i = 0;
		for(EventListeners l : children)
		{
			childrenBaked[i] = l;
			i++;
		}
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

	public EventListeners[] getChildrens()
	{
		return childrenBaked;
	}
}
