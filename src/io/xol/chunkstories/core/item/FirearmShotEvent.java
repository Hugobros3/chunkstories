package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class FirearmShotEvent extends Event
{
	// Every event class has to have this

	static EventListeners listeners = new EventListeners(FirearmShotEvent.class);

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

	final ItemFirearm itemFirearm;
	final EntityLiving shooter;
	final Controller controller;

	public FirearmShotEvent(ItemFirearm itemFirearm, EntityLiving shooter, Controller controller)
	{
		this.itemFirearm = itemFirearm;
		this.shooter = shooter;
		this.controller = controller;
	}

	public ItemFirearm getItemFirearm()
	{
		return itemFirearm;
	}

	public EntityLiving getShooter()
	{
		return shooter;
	}

	public Controller getController()
	{
		return controller;
	}
}
