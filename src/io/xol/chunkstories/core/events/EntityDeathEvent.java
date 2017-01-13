package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * This event is called upon confirmed death of a living entity.
 * You can't and shouldn't prevent it from dying here, instead use the EntityDamageEvent to cancel the damage.
 */
public class EntityDeathEvent extends Event
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
	
	EntityLiving entity;
	
	public EntityDeathEvent(EntityLiving entity)
	{
		this.entity = entity;
	}
	
	public EntityLiving getEntity()
	{
		return entity;
	}
}
