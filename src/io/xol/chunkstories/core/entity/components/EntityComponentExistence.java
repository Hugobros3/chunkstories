package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.world.WorldMaster;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * This component is just a flag describing the existence of an entity.
 */
public final class EntityComponentExistence extends EntityComponent
{
	public EntityComponentExistence(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	boolean exists = true;
	
	public boolean exists()
	{
		return exists;
	}
	
	public void destroyEntity()
	{
		exists = false;
		this.pushComponentEveryone();
	}
	
	@Override
	public void push(StreamTarget to, DataOutputStream dos) throws IOException
	{
		if(to instanceof Subscriber)
		{
			if(!((Subscriber)to).isSubscribedTo(entity))
			{
				System.out.println(to+" is not in the subscribers list, telling him the entity doesn't exist anymore >:D");
				dos.writeBoolean(false);
				return;
			}
		}
		dos.writeBoolean(exists);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		boolean existedBefore = exists;
		boolean willExist = dis.readBoolean();
		//If the entity existedBefore and we are not the world master, we remove it from our world
		if(!willExist && existedBefore && !(entity.getWorld() instanceof WorldMaster))
		{
			//System.out.println("Debug : removed entity "+entity + "from world");
			entity.removeFromWorld();
		}
	}

}
