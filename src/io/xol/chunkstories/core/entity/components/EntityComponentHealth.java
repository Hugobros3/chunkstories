package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.events.EntityDeathEvent;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentHealth extends EntityComponent
{
	public float value;
	
	public EntityComponentHealth(Entity entity, float health)
	{
		super(entity);
		this.value = health;
	}

	public float getHealth()
	{
		return value;
	}
	
	public void setHealth(float health)
	{
		boolean wasntDead = health > 0.0;
		this.value = health;
		
		if(health < 0.0 && wasntDead)
		{
			EntityDeathEvent entityDeathEvent = new EntityDeathEvent(entity);
			entity.getWorld().getGameLogic().getPluginsManager().fireEvent(entityDeathEvent);
		}
		
		if(entity.getWorld() instanceof WorldMaster)
		{
			if(health > 0.0)
				this.pushComponentController();
			else
				this.pushComponentEveryone();
		}
	}
	
	public void damage(float dmg)
	{
		boolean wasntDead = value > 0.0;
		this.value -= dmg;

		if(value < 0.0 && wasntDead)
		{
			EntityDeathEvent entityDeathEvent = new EntityDeathEvent(entity);
			entity.getWorld().getGameLogic().getPluginsManager().fireEvent(entityDeathEvent);
		}
		
		if(entity.getWorld() instanceof WorldMaster)
		{
			if(value > 0.0)
				this.pushComponentController();
			else
				this.pushComponentEveryone();
		}
	}
	
	@Override
	public void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeFloat(value);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		value = dis.readFloat();
	}

}
