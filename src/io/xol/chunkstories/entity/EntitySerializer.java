package io.xol.chunkstories.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.serialization.OfflineSerializedData;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.chunkstories.tools.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.tools.ChunkStoriesLogger.LogType;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The one place in the code where we serialize entities
 */
public class EntitySerializer
{
	public static void writeEntityToStream(DataOutputStream out, OfflineSerializedData destination, Entity entity)
	{
		try
		{
			out.writeLong(entity.getUUID());
			out.writeShort(entity.getEID());

			//Write all components we wanna update
			entity.getComponents().pushAllComponentsInStream(destination, out);

			//Then write 0 to mark end of components
			out.writeInt((int) 0);

			//System.out.println("Wrote serialized "+entity.getClass().getSimpleName()+" to : "+destination + " note: "+entity.exists());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static Entity readEntityFromStream(DataInputStream in, OfflineSerializedData source, World world)
	{
		try
		{
			long entityUUID = in.readLong();
			
			//When we reach id -1 in a stream of entities, it means we reached the end.
			if(entityUUID == -1)
				return null;
			
			short entityTypeID = in.readShort();
			
			/*System.out.println("world"+world);
			System.out.println("world.getGameContext()"+world.getGameContext());
			System.out.println("world.getGameContext().getContent().entities()"+world.getGameContext().getContent().entities());
			System.out.println("world.getGameContext().getContent().entities().getEntityTypeById(entityTypeID)"+world.getGameContext().getContent().entities().getEntityTypeById(entityTypeID));
			*/
			Entity entity = world.getGameContext().getContent().entities().getEntityTypeById(entityTypeID).create(world);
			entity.setUUID(entityUUID);
			
			int componentId = in.readInt();
			//Loop throught all components
			while(true)
			{
				if(componentId == 0) // End of components to read
					break;
				else if(componentId == -1)
				{
					//Read UTF-8 component name
					String componentName = in.readUTF();
					try {
						entity.getComponents().tryPullComponentInStream(componentName, source, in);
					}
					catch(UnknownComponentException e) {
						ChunkStoriesLogger.getInstance().log(e.getMessage(), LogType.INTERNAL, LogLevel.WARN);
					}
				}
				else
				{
					//Read int32 component id
					try {
						entity.getComponents().tryPullComponentInStream(componentId, source, in);
					}
					catch(UnknownComponentException e) {
						ChunkStoriesLogger.getInstance().log(e.getMessage(), LogType.INTERNAL, LogLevel.WARN);
					}
				}
				componentId = in.readInt();
			}
			
			return entity;
		}
		catch (NullPointerException | IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
