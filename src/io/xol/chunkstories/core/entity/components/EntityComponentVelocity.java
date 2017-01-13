package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.net.packets.PacketVelocityDelta;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentVelocity extends EntityComponent
{
	public EntityComponentVelocity(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	private Vector3dm velocity = new Vector3dm();
	
	public Vector3dm getVelocity()
	{
		return velocity;
	}

	public void setVelocity(Vector3dm velocity)
	{
		this.velocity.setX(velocity.getX());
		this.velocity.setY(velocity.getY());
		this.velocity.setZ(velocity.getZ());
		
		this.pushComponentEveryone();
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

	public void addVelocity(Vector3dm delta)
	{
		this.velocity.add(delta);
		
		this.pushComponentEveryoneButController();
		//Notify the controller otherwise: 
		if(entity instanceof EntityControllable)
		{
			Controller controller = ((EntityControllable) entity).getControllerComponent().getController();
			if(controller != null)
			{
				PacketVelocityDelta packet = new PacketVelocityDelta(delta);
				controller.pushPacket(packet);
			}
		}
	}

	public void addVelocity(double x, double y, double z)
	{
		this.addVelocity(new Vector3dm(x, y, z));
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
