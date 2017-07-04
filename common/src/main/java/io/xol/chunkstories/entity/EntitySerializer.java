package io.xol.chunkstories.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.serialization.OfflineSerializedData;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogLevel;
import io.xol.chunkstories.api.util.ChunkStoriesLogger.LogType;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.entity.LengthAwareBufferedIOHelper.LengthAwareOutputStream;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * The one place in the code where we serialize entities
 */
public class EntitySerializer
{
	
	
	public static void writeEntityToStream(DataOutputStream dos, OfflineSerializedData destination, Entity entity)
	{
		try
		{
			if(entity == null) {
				dos.writeInt(-1);
				return;
			}
			
			LengthAwareOutputStream out = LengthAwareBufferedIOHelper.getLengthAwareOutput();
			
			out.writeLong(entity.getUUID());
			out.writeShort(entity.getEID());

			//Write all components we wanna update
			entity.getComponents().pushAllComponentsInStream(destination, out);

			//Then write 0 to mark end of components
			out.writeInt((int) 0);

			//Flush the deal
			out.writeTheStuffPrecededByLength(dos);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static Entity readEntityFromStream(DataInputStream dis, OfflineSerializedData source, World world)
	{
		try
		{
			int entityDataLength = dis.readInt();
			
			if(entityDataLength == -1)
				return null;
			
			DataInputStream in = LengthAwareBufferedIOHelper.getLengthAwareInput(entityDataLength, dis);
			
			long entityUUID = in.readLong();
			
			//Obsolete ?
			//When we reach id -1 in a stream of entities, it means we reached the end.
			//if(entityUUID == -1)
			//	return null;
			
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
						ChunkStoriesLoggerImplementation.getInstance().log("Failure reading component "+componentName + " from "+source, LogType.INTERNAL, LogLevel.WARN);
						ChunkStoriesLoggerImplementation.getInstance().log(e.getMessage(), LogType.INTERNAL, LogLevel.WARN);
					}
				}
				else
				{
					//Read int32 component id
					try {
						entity.getComponents().tryPullComponentInStream(componentId, source, in);
					}
					catch(UnknownComponentException e) {
						ChunkStoriesLoggerImplementation.getInstance().log(e.getMessage(), LogType.INTERNAL, LogLevel.WARN);
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
