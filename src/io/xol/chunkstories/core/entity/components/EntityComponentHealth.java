package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentHealth extends EntityComponent
{
	public float health;
	
	public EntityComponentHealth(Entity entity, float health)
	{
		super(entity);
		this.health = health;
	}

	public float getHealth()
	{
		return health;
	}
	
	public void setHealth(float health)
	{
		this.health = health;
		this.pushComponentController();
	}
	
	public void damage(float dmg)
	{
		this.health -= dmg;
		this.pushComponentController();
	}
	
	@Override
	public void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeFloat(health);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		health = dis.readFloat();
	}

}
