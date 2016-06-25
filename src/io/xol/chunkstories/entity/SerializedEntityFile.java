package io.xol.chunkstories.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.OfflineSerializedData;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.world.World;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SerializedEntityFile implements OfflineSerializedData
{
	private final File file;
	
	public SerializedEntityFile(String string)
	{
		file = new File(string);
	}
	
	public boolean exists()
	{
		return file.exists();
	}
	
	public String toString()
	{
		return "[CSF File: "+file.toString()+"]";
	}
	
	public Entity read(World world)
	{
		try
		{
			DataInputStream in = new DataInputStream(new FileInputStream(file));
			
			long entityUUID = in.readLong();
			short entityTypeID = in.readShort();
			
			Entity entity = EntitiesList.newEntity(world, entityTypeID);
			entity.setUUID(entityUUID);
			
			int componentId = in.readInt();
			//Loop throught all components
			while(componentId != 0)
			{
				if(!entity.getComponents().tryPullComponentInStream(componentId, this, in))
					throw new UnknownComponentException(componentId, entity.getClass());
				componentId = in.readInt();
			}
			
			System.out.println("Read serialized entity from : "+file);
			
			in.close();
			
			return entity;
		}
		catch (IOException | UnknownComponentException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public void write(Entity entity)
	{
		try
		{
			DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
			
			out.writeLong(entity.getUUID());
			out.writeShort(entity.getEID());
			
			//Write all components we wanna update
			entity.getComponents().pushAllComponentsInStream(this, out);
			
			//Then write 0
			out.writeInt((int)0);
			
			out.flush();
			out.close();
			
			System.out.println("Wrote serialized entity to : "+file);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
