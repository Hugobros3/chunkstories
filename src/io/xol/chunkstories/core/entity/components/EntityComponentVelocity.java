package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentVelocity extends EntityComponent
{
	public EntityComponentVelocity(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	private Vector3d velocity = new Vector3d();
	
	public Vector3d getVelocity()
	{
		return velocity;
	}

	public void setVelocity(Vector3d velocity)
	{
		this.velocity.setX(velocity.getX());
		this.velocity.setY(velocity.getY());
		this.velocity.setZ(velocity.getZ());
	}

	public void setVelocity(double x, double y, double z)
	{
		this.velocity.setX(x);
		this.velocity.setY(y);
		this.velocity.setZ(z);
		
		this.pushComponentEveryone();
	}

	public void setVelocityX(double x)
	{
		this.velocity.setX(x);
		
		this.pushComponentEveryone();
	}

	public void setVelocityY(double y)
	{
		this.velocity.setY(y);
		
		this.pushComponentEveryone();
	}

	public void setVelocityZ(double z)
	{
		this.velocity.setZ(z);
		
		this.pushComponentEveryone();
	}

	public void addVelocity(double x, double y, double z)
	{
		this.velocity.add(x, y, z);
		
		this.pushComponentEveryone();
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeDouble(velocity.getX());
		dos.writeDouble(velocity.getY());
		dos.writeDouble(velocity.getZ());
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		velocity.setX(dis.readDouble());
		velocity.setY(dis.readDouble());
		velocity.setZ(dis.readDouble());
		
		this.pushComponentEveryoneButController();
	}
}
