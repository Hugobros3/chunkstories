package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.entity.EntityTypesStore;

import java.io.DataInputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
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
		{
			//System.out.println("Sending an update about a non-existent entity to "+destinator+", appending with existence component for clarification.");
			//System.out.println("We should avoid doing that btw.");
			//Thread.dumpStack();
			entityToUpdate.getComponentExistence().pushComponentInStream(destinator, this.getSynchPacketOutputStream());
		}
		
		//Write a 0 to mark the end of the components updates
		this.getSynchPacketOutputStream().writeInt(0);
		
		//Finalizes the synch packet and lets the game build another
		this.finalizeSynchPacket();
	}
	
	/*public void secnd(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//No other updates than the entity destruction itself can be sent once it has been removed
		if(!entityToUpdate.exists() && updateOneComponent != null && !(updateOneComponent instanceof EntityComponentExistence))
		{
			//This is done because if the packets were allowed to filter the clients would create an entity to write the attributes into but they'd never hear from it
			//ever again
			
			//System.out.println("WOW ! Tried to send something about a ghost entity there !");
			//Thread.currentThread().dumpStack();
			
			entityTypeID = -1;
			updateOneComponent = null;
		}
		
		entityUUID = entityToUpdate.getUUID();
		entityTypeID = entityToUpdate.getEID();
		
		//System.out.println("Sending entity " + entityID + " EID : " + entityType + " PosX" + XBuffered + (nBuffered == null ? "null" : nBuffered));
		out.writeLong(entityUUID);
		out.writeShort(entityTypeID);
		
		//Write all components we wanna update
		if(updateOneComponent != null)
			updateOneComponent.pushComponentInStream(destinator, out);
		else if(updateManyComponents != null)
			updateManyComponents.pushAllComponentsInStream(destinator, out);
		
		//Then write 0
		out.writeInt((int)0);
	}*/

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, UnknownComponentException
	{
		entityUUID = in.readLong();
		entityTypeID = in.readShort();
		
		if(entityTypeID == -1)
			return;
		
		//((WorldImplementation)processor.getWorld()).entitiesLock.writeLock().lock();
		Entity entity = processor.getWorld().getEntityByUUID(this.entityUUID);
		
		boolean addToWorld = false;
		//Create an entity if the servers tells you to do so
		if(entity == null)
		{
			entity = processor.getWorld().getGameContext().getContent().entities().getEntityTypeById(entityTypeID).create(processor.getWorld());
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
			//System.out.println("add entity");
			//Only the WorldMaster is allowed to spawn new entities in the world
			if(processor.isClient)
				processor.getWorld().addEntity(entity);
		}
		//lock.unlock();
		//((WorldImplementation)processor.getWorld()).entitiesLock.writeLock().unlock();
		//((WorldImplementation)processor.getWorld()).entitiesLock.unlock();
	}
}
