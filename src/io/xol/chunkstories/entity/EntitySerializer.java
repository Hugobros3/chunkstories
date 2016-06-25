package io.xol.chunkstories.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.OfflineSerializedData;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntitySerializer
{
	public void writeEntityToStream(DataOutputStream out, OfflineSerializedData destination, Entity entity)
	{
		try
		{
			out.writeLong(entity.getUUID());
			out.writeShort(entity.getEID());
			
			//Write all components we wanna update
			entity.getComponents().pushAllComponentsInStream(destination, out);
			
			//Then write 0 to mark end of components
			out.writeInt((int)0);
			
			out.flush();
			out.close();
			
			System.out.println("Wrote serialized entity to : "+destination);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public Entity readEntityFromStream(DataInputStream in, OfflineSerializedData source, World world)
	{
		try
		{
			long entityUUID = in.readLong();
			short entityTypeID = in.readShort();
			
			Entity entity = EntitiesList.newEntity(world, entityTypeID);
			entity.setUUID(entityUUID);
			
			int componentId = in.readInt();
			//Loop throught all components
			while(componentId != 0)
			{
				if(!entity.getComponents().tryPullComponentInStream(componentId, source, in))
					throw new UnknownComponentException(componentId, entity.getClass());
				componentId = in.readInt();
			}
			
			System.out.println("Read serialized entity from : "+source);
			
			return entity;
		}
		catch (IOException | UnknownComponentException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
