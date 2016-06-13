package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.entity.EntitiesList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketEntity extends Packet
{
	public short entityTypeID;
	public long entityUUID;
	
	public EntityComponent updateOneComponent;
	public EntityComponent updateManyComponents;
	
	public PacketEntity(boolean client)
	{
		super(client);
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		//System.out.println("Sending entity " + entityID + " EID : " + entityType + " PosX" + XBuffered + (nBuffered == null ? "null" : nBuffered));
		out.writeLong(entityUUID);
		out.writeShort(entityTypeID);
		
		//Write all components we wanna update
		if(updateOneComponent != null)
		{
			updateOneComponent.pushComponentInStream(destinator, out);
		}
		else
			updateManyComponents.pushAllComponentsInStream(destinator, out);
		
		//Then write 0
		out.writeInt((int)0);
	}

	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, UnknownComponentException
	{
		entityUUID = in.readLong();
		entityTypeID = in.readShort();
		
		Entity entity = processor.getWorld().getEntityByUUID(this.entityUUID);
		
		boolean addToWorld = false;
		//Create an entity if the servers tells you to do so
		if(entity == null)
		{
			entity = EntitiesList.newEntity(processor.getWorld(), this.entityTypeID);
			entity.setUUID(entityUUID);
			
			addToWorld = true;
		}
		
		int componentId = in.readInt();
		//Loop throught all components
		while(componentId != 0)
		{
			//System.out.println("got component : "+componentId);
			if(!entity.getComponents().tryPullComponentInStream(componentId, sender, in))
				throw new UnknownComponentException(componentId, entity.getClass());
			componentId = in.readInt();
		}
		
		if(addToWorld)
		{
			//Only the WorldMaster is allowed to spawn new entities in the world
			if(processor.isClient)
				processor.getWorld().addEntity(entity);
		}
	}
}
