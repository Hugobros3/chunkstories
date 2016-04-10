package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.entity.EntityControllable;
import io.xol.chunkstories.entity.EntityImpl;
import io.xol.chunkstories.entity.EntityNameable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class PacketEntity extends Packet
{
	/**
	 * Transfers essential data about entities
	 */
	//private Entity entity;
	public short entityTypeID;
	public long entityUUID;
	//World world;

	public double XBuffered, YBuffered, ZBuffered;
	public double RHBuffered, RVBuffered;
	public String nBuffered;

	public boolean defineControl = false; // Tells the client that the player entity is this one.
	public boolean includeRotation = false; // Tells both sides to consider extra 2 doubles for yaw and pitch
	public boolean includeName = false; // This is a nameable entity
	public boolean deleteFlag = false; // Tells client to stop tracking this entity and delete it
	public boolean includeVelocity = false; // Include or not position interpolation

	public PacketEntity(boolean client)
	{
		super(client);
	}

	@Override
	public void send(DataOutputStream out) throws IOException
	{
		//System.out.println("Sending entity " + entityID + " EID : " + entityType + " PosX" + XBuffered + (nBuffered == null ? "null" : nBuffered));
		out.writeLong(entityUUID);
		out.writeShort(entityTypeID);
		out.writeDouble(XBuffered);
		out.writeDouble(YBuffered);
		out.writeDouble(ZBuffered);
		byte byteField = 0x00;
		byteField = (byte) (byteField | ((defineControl ? 0x01 : 0x00) << 0));
		byteField = (byte) (byteField | ((includeRotation ? 0x01 : 0x00) << 1));
		byteField = (byte) (byteField | ((includeName ? 0x01 : 0x00) << 2));
		byteField = (byte) (byteField | ((deleteFlag ? 0x01 : 0x00) << 3));
		out.writeByte(byteField);
		if (includeRotation)
		{
			out.writeDouble(RHBuffered);
			out.writeDouble(RVBuffered);
		}
		if (includeName)
		{
			/*String name = "ERROR-NOTNAMEABLE";
			if(entity instanceof EntityNameable)
				name = ((EntityNameable)entity).getName();*/
			out.writeUTF(nBuffered);
		}
	}

	@Override
	public void read(DataInputStream in) throws IOException
	{
		entityUUID = in.readLong();
		entityTypeID = in.readShort();

		XBuffered = in.readDouble();
		YBuffered = in.readDouble();
		ZBuffered = in.readDouble();

		byte byteField = in.readByte();
		defineControl = ((byteField >> 0) & 0x01) == 1;
		includeRotation = ((byteField >> 1) & 0x01) == 1;
		includeName = ((byteField >> 2) & 0x01) == 1;
		deleteFlag = ((byteField >> 3) & 0x01) == 1;

		if (includeRotation)
		{
			RHBuffered = (float) in.readDouble();
			RVBuffered = (float) in.readDouble();
		}
		if (includeName)
		{
			nBuffered = in.readUTF();
			System.out.println(nBuffered);
		}
	}

	public void applyToEntity(Entity entity)
	{
		if(!(entity instanceof EntityImpl))
			return;
		EntityImpl impl = (EntityImpl)entity;
		impl.pos.x = XBuffered;
		impl.pos.y = YBuffered;
		impl.pos.z = ZBuffered;
		if (includeRotation)
		{
			impl.rotH = (float) RHBuffered;
			impl.rotV = (float) RVBuffered;
		}
		if (includeName)
		{
			//System.out.println("apply 2 "+entity+" posx"+XBuffered+" -> "+nBuffered);
			if (entity instanceof EntityNameable)
				((EntityNameable) entity).setName(nBuffered);
		}
		//System.out.println("apply 2 "+entity+" posx"+XBuffered+" -> "+XBuffered);
	}

	public void createFromEntity(Entity entity)
	{
		entityTypeID = entity.getEID();
		entityUUID = entity.getUUID();

		Location loc = entity.getLocation();
		XBuffered = loc.x;
		YBuffered = loc.y;
		ZBuffered = loc.z;
		if (includeRotation && entity instanceof EntityImpl)
		{
			EntityImpl impl = (EntityImpl)entity;
			RHBuffered = impl.rotH;
			RVBuffered = impl.rotV;
		}
		if (includeName)
		{
			if (entity instanceof EntityNameable)
				nBuffered = ((EntityNameable) entity).getName();
		}
	}

	@Override
	public void process(PacketsProcessor processor)
	{
		if(processor.isClient)
		{
			EntityImpl entity = (EntityImpl) Client.world.getEntityByUUID(this.entityUUID);
			if(this.deleteFlag)
				Client.world.removeEntity(entity);
			else
			{
				//Create an entity if the servers tells you to do so
				if(entity == null)
				{
					entity = (EntityImpl) EntitiesList.newEntity(Client.world, this.entityTypeID);
					entity.entityID = this.entityUUID;
					this.applyToEntity(entity);
					Client.world.addEntity(entity);
					//
					
				}
				else
					this.applyToEntity(entity);
				//Moved here so we can tell the client to control an already existing entity
				if(this.defineControl)
				{
					if(entity != null)
					{
						Client.controlledEntity = entity;
						if(entity instanceof EntityControllable)
							((EntityControllable) entity).setController(Client.getInstance());
					}
					//else
					//	Client.getInstance().printChat("Error: Server gave control of unknown entity: "+entityUUID);
				}
			}
		}
		else
		{
			//Client isn't allowed to force spawning or moving of anything but himself
			if (processor.getServerClient().getProfile().getControlledEntity() != null && entityUUID == processor.getServerClient().getProfile().getControlledEntity().getUUID())
				applyToEntity(processor.getServerClient().getProfile().getControlledEntity());
			//entity = EntitiesList.newEntity(world, entityType);
		}
	}
}
