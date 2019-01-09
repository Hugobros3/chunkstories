//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.entity;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.Trait;
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.serialization.OfflineSerializedData;
import xyz.chunkstories.entity.LengthAwareBufferedIOHelper.LengthAwareOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The one place in the code where we serialize entities
 */

//TODO write component data length and use it to avoid getting fucked when components change
public class EntitySerializer {
    public static void writeEntityToStream(DataOutputStream dos, OfflineSerializedData destination, Entity entity) {
        try {
            //If there is no entity, we write zero. Used by the file format to know when the entities section end
            if (entity == null) {
                dos.writeByte(0);
                return;
            }
            dos.writeByte(1);

            dos.writeLong(entity.getUUID());
            dos.writeShort(entity.getWorld().getContentTranslator().getIdForEntity(entity));

            // Write all components we wanna update
            for (Trait trait : entity.traits.all()) {
                if (trait instanceof TraitSerializable) {
                    LengthAwareOutputStream out = LengthAwareBufferedIOHelper.getLengthAwareOutput();
                    ((TraitSerializable) trait).pushComponentInStream(destination, out);

                    if (out.size() > 0)
                        out.writeTheStuffPrecededByLength(dos);
                    else {
                        logger.warn("Component " + trait + " wrote nothing.");
                    }
                }
            }

            dos.writeInt(-1);

            // Flush the deal
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
            byte presence = dis.readByte();
            if (presence == 0)
                return null;

            long entityUUID = dis.readLong();
            short entityTypeID = dis.readShort();
            Entity entity = world.getContentTranslator().getEntityForId(entityTypeID).newEntity(world);
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
                    for (Trait trait : entity.traits.all()) {
                        if (trait instanceof TraitSerializable
                                && ((TraitSerializable) trait).name.equals(componentName)) {
                            try {
                                ((TraitSerializable) trait).tryPull(source, in);
                            } catch (IOException e) {
                                logger().warn("Failure reading component " + componentName + " from " + source);
                                logger().warn(e.getMessage());
                            }
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        logger.error("Error: could not find a reader for component: " + componentName);
                    }

                } else {
                    // Read int32 component id
                    try {
                        Trait trait = entity.traits.byId()[componentId];
                        if (trait instanceof TraitSerializable)
                            ((TraitSerializable) trait).tryPull(source, in);
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
