package io.xol.chunkstories.core.events;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;
import io.xol.chunkstories.api.server.Player;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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
	
	Entity entity;
	
	public EntityDeathEvent(Entity entity)
	{
		this.entity = entity;
	}
	
	@Override
	public void defaultBehaviour()
	{
		if(entity instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) entity).getControllerComponent().getController();
			if(controller != null)
				controller.setControlledEntity(null);
			
			if(controller instanceof Player)
			{
				Player player = (Player)controller;
				
				PlayerDeathEvent event = new PlayerDeathEvent(player);
				entity.getWorld().getGameLogic().getPluginsManager().fireEvent(event);
			}
		}
	}
	
	public Entity getEntity()
	{
		return entity;
	}
}
