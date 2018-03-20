//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.exceptions.UnknownComponentException;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.serialization.OfflineSerializedData;
import io.xol.chunkstories.entity.LengthAwareBufferedIOHelper.LengthAwareOutputStream;

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
			out.writeShort(entity.getWorld().getContentTranslator().getIdForEntity(entity));

			//Write all components we wanna update
			entity.getComponents().pushAllComponentsInStream(destination, out);

			//Then write 0 to mark end of components
			out.writeInt((int) 0);

			//Flush the deal
			out.writeTheStuffPrecededByLength(dos);
		}
		catch(NullPointerException e)
		{
			System.out.println(entity.getClass().getName());
			System.out.println(entity.getDefinition());
			e.printStackTrace();
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
			Entity entity = world.getContentTranslator().getEntityForId(entityTypeID).create(new Location(world, 0d, 0d, 0d));
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
						logger().warn("Failure reading component "+componentName + " from "+source);
						logger().warn(e.getMessage());
					}
				}
				else
				{
					//Read int32 component id
					try {
						entity.getComponents().tryPullComponentInStream(componentId, source, in);
					}
					catch(UnknownComponentException e) {
						logger().warn(e.getMessage());
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
	
	private static final Logger logger = LoggerFactory.getLogger("world.serialization.entity");
	public static Logger logger() {
		return logger;
	}
}
