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
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.serialization.OfflineSerializedData;
import io.xol.chunkstories.entity.LengthAwareBufferedIOHelper.LengthAwareOutputStream;

/**
 * The one place in the code where we serialize entities
 */

//TODO write length and use it to avoid getting fucked when components change
public class EntitySerializer {
	public static void writeEntityToStream(DataOutputStream dos, OfflineSerializedData destination, Entity entity) {
		try {
			if (entity == null) {
				dos.writeByte(0);
				return;
			}
			dos.writeByte(1);

			dos.writeLong(entity.getUUID());
			dos.writeShort(entity.getWorld().getContentTranslator().getIdForEntity(entity));

			// Write all components we wanna update
			for (EntityComponent c : entity.components.all()) {
				LengthAwareOutputStream out = LengthAwareBufferedIOHelper.getLengthAwareOutput();
				c.pushComponentInStream(destination, out);
				
				if(out.size() > 0)
					out.writeTheStuffPrecededByLength(dos);
				else {
					logger.warn("Component "+c+" wrote nothing.");
				}
			}

			// Then write -1 to mark end of components
			dos.writeInt((int) -1);

			// Flush the deal
			//out.writeTheStuffPrecededByLength(dos);
		} catch (NullPointerException e) {
			System.out.println(entity.getClass().getName());
			System.out.println(entity.getDefinition());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Entity readEntityFromStream(DataInputStream dis, OfflineSerializedData source, World world) {
		try {
			/*int entityDataLength = dis.readInt();

			if (entityDataLength == -1)
				return null;*/
			byte presence = dis.readByte();
			if(presence == 0)
				return null;

			long entityUUID = dis.readLong();
			short entityTypeID = dis.readShort();
			Entity entity = world.getContentTranslator().getEntityForId(entityTypeID).create(new Location(world, 0d, 0d, 0d));
			entity.setUUID(entityUUID);

			// Loop throught all components
			while (true) {
				int componentLength = dis.readInt();
				if (componentLength == -1) // End of components to read
					break;
				
				DataInputStream in = LengthAwareBufferedIOHelper.getLengthAwareInput(componentLength, dis);
				
				int componentId = in.readInt();
				if (componentId == -1) {
					// Read UTF-8 component name
					String componentName = in.readUTF();

					boolean found = false;
					for (EntityComponent c : entity.components.all()) {
						if (c.name.equals(componentName)) {
							try {
								c.tryPull(source, in);
							} catch (IOException e) {
								logger().warn("Failure reading component " + componentName + " from " + source);
								logger().warn(e.getMessage());
							}
							found = true;
							break;
						}
					}
					
					if(!found) {
						logger.error("Error: could not find a reader for component: "+componentName);
					}

				} else {
					// Read int32 component id
					try {
						entity.components.byId()[componentId].tryPull(source, in);
						// entity.getComponents().tryPullComponentInStream(componentId, source, in);
					} catch (IOException e) {
						logger().warn("Failure reading component #" + componentId + " from " + source);
						logger().warn(e.getMessage());
					}
				}
			}

			return entity;
		} catch (NullPointerException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static final Logger logger = LoggerFactory.getLogger("world.serialization.entity");

	public static Logger logger() {
		return logger;
	}
}
