package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.net.StreamSource;
import io.xol.chunkstories.api.net.StreamTarget;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.World;
import io.xol.chunkstories.world.chunk.ChunkHolder;
import io.xol.engine.concurrency.SafeWriteLock;
import io.xol.engine.math.lalgb.Vector3d;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentPosition extends EntityComponent
{
	public EntityComponentPosition(Entity entity, EntityComponent next)
	{
		super(entity, next);
	}

	SafeWriteLock safetyLock = new SafeWriteLock();
	private Location pos = new Location(entity.getWorld(), 0, 0, 0);
	
	//TODO remake-me
	private ChunkHolder parentHolder;

	public Location getLocation()
	{
		//safetyLock.beginRead();
		Location pos = this.pos;
		//safetyLock.endRead();
		return new Location(pos.getWorld(), pos);
	}
	
	public void setLocation(Location location)
	{	
		assert location != null;
		
		this.pos = location;
		//Push updates to everyone subscribed to this
		//In client mode it means that the controlled entity has the server subscribed so it will update it's status to the server
		
		//In server/master mode any drastic location change is told to everyone, as setLocation() is not called when the server receives updates
		//from the controller but only when an external event changes the location.
		this.pushComponentEveryone();
	}
	
	public void setPositionXYZ(double x, double y, double z)
	{
		this.pos.x = x;
		this.pos.y = y;
		this.pos.z = z;
		
		checkPositionAndUpdateHolder();

		//Same logic as above, refactoring should be done for clarity tho
		this.pushComponentEveryone();
	}
	
	public void setPosition(Vector3d position)
	{
		this.pos.x = position.x;
		this.pos.y = position.y;
		this.pos.z = position.z;
		
		checkPositionAndUpdateHolder();

		//Same logic as above, refactoring should be done for clarity tho
		this.pushComponentEveryone();
	}

	@Override
	public void push(StreamTarget to, DataOutputStream dos) throws IOException
	{
		dos.writeDouble(pos.x);
		dos.writeDouble(pos.y);
		dos.writeDouble(pos.z);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		if(pos == null)
			pos = new Location(this.entity.getWorld(), 0, 0, 0);
		
		pos.setX(dis.readDouble());
		pos.setY(dis.readDouble());
		pos.setZ(dis.readDouble());
		
		//System.out.println(entity.getUUID() + Client.username);
		
		//if(entity.getUUID() == 0)
		//	System.out.println("being cucked" + pos);
		
		checkPositionAndUpdateHolder();
		
		//Position updates received by the server should be told to everyone but the controller
		if(entity.getWorld() instanceof WorldMaster)
			pushComponentEveryoneButController();
	}
	
	/**
	 * Prevents entities from going outside the world area and updates the parentHolder reference
	 */
	protected final boolean checkPositionAndUpdateHolder()
	{
		pos.x %= entity.getWorld().getWorldSize();
		pos.z %= entity.getWorld().getWorldSize();
		if (pos.x < 0)
			pos.x += entity.getWorld().getWorldSize();
		if (pos.z < 0)
			pos.z += entity.getWorld().getWorldSize();
		int regionX = (int) (pos.x / (32 * 8));
		int regionY = (int) (pos.y / (32 * 8));
		if (regionY < 0)
			regionY = 0;
		if (regionY > entity.getWorld().getMaxHeight() / (32 * 8))
			regionY = entity.getWorld().getMaxHeight() / (32 * 8);
		int regionZ = (int) (pos.z / (32 * 8));
		if (parentHolder != null && parentHolder.regionX == regionX && parentHolder.regionY == regionY && parentHolder.regionZ == regionZ)
		{
			return false; // Nothing to do !
		}
		else
		{
			//if(parentHolder != null)
			//	parentHolder.removeEntity(this);
			
			//TODO refactor using regions
			
			//parentHolder = entity.getWorld().getChunkHolder(regionX * 8, regionY * 8, regionZ * 8, true);
			
			//parentHolder.addEntity(this);
			/*System.out.println("Had to move entity "+this+" to a new holder :");
			System.out.println("RegionX : "+regionX+" PH: "+parentHolder.regionX);
			System.out.println("RegionY : "+regionY+" PH: "+parentHolder.regionY);
			System.out.println("RegionZ : "+regionZ+" PH: "+parentHolder.regionZ);*/
			return true;
		}
	}

}
