package io.xol.chunkstories.api.net.packets;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogType;
import io.xol.chunkstories.api.world.World;

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
		
		//System.out.println("PacketEntity");
		
		if(entityTypeID == -1)
			return;
		
		//System.out.println("entityTypeID"+entityTypeID);
		
		World world = processor.getWorld();
		if(world == null)
			return;
		
		//System.out.println("world"+world);
		
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
			//if(processor.getWorld() instanceof WorldClient)
			//	System.out.println("OKKKKKKKKKKKKKK cId"+componentId);
			
			try {
				entity.getComponents().tryPullComponentInStream(componentId, sender, in);
			}
			catch(UnknownComponentException e) {
				
				processor.getContext().logger().log(e.getMessage(), LogType.INTERNAL, LogLevel.WARN);
				//ChunkStoriesLogger.getInstance().log(e.getMessage(), LogType.INTERNAL, LogLevel.WARN);
			}
			componentId = in.readInt();
		}
		
		if(addToWorld && entity.exists())
		{
			//Only the WorldMaster is allowed to spawn new entities in the world
			if(processor instanceof ClientPacketsProcessor)
				processor.getWorld().addEntity(entity);
		}
	}
}
