package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.entity.EntityTypesStore;

import java.io.DataInputStream;
import java.io.IOException;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketEntity extends PacketSynch implements PacketPrepared
{
	private short entityTypeID;
	private long entityUUID;

	private Entity entityToUpdate;

	public PacketEntity()
	{
		
	}
	
	public PacketEntity(Entity entityToUpdate) throws IOException
	{
		this.entityToUpdate = entityToUpdate;
		
		entityUUID = entityToUpdate.getUUID();
		entityTypeID = entityToUpdate.getEID();
		
		this.getSynchPacketOutputStream().writeLong(entityUUID);
		this.getSynchPacketOutputStream().writeShort(entityTypeID);
	}

	@Override
	public void prepare(PacketDestinator destinator) throws IOException
	{
		//If the entity no longer exists, we make sure we tell the player so he doesn't spawn it again
		if(!entityToUpdate.exists())
			entityToUpdate.getComponentExistence().pushComponentInStream(destinator, this.getSynchPacketOutputStream());
		
		//Write a 0 to mark the end of the components updates
		this.getSynchPacketOutputStream().writeInt(0);
		
		//Finalizes the synch packet and lets the game build another
		this.finalizeSynchPacket();
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, UnknownComponentException
	{
		entityUUID = in.readLong();
		entityTypeID = in.readShort();
		
		if(entityTypeID == -1)
			return;
		
		World world = processor.getWorld();
		if(world == null)
			return;
		
		Entity entity = world.getEntityByUUID(this.entityUUID);
		
		boolean addToWorld = false;
		//Create an entity if the servers tells you to do so
		if(entity == null)
		{
			entity = processor.getWorld().
					getGameContext().
					getContent().
					entities().
					getEntityTypeById(entityTypeID).
					create(processor.getWorld());
					//Entities.newEntity(processor.getWorld(), this.entityTypeID);
			entity.setUUID(entityUUID);
			
			addToWorld = true;
		}
		
		int componentId = in.readInt();
		//Loop throught all components
		while(componentId != 0)
		{
			if(!entity.getComponents().tryPullComponentInStream(componentId, sender, in))
				throw new UnknownComponentException(componentId, entity.getClass());
			componentId = in.readInt();
		}
		
		if(addToWorld && entity.exists())
		{
			//Only the WorldMaster is allowed to spawn new entities in the world
			if(processor.isClient)
				processor.getWorld().addEntity(entity);
		}
	}
}
