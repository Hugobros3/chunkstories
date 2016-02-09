package io.xol.chunkstories.net.packets;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.entity.Entity;
import io.xol.chunkstories.entity.EntityNameable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Packet04Entity extends Packet
{
	/**
	 * Transfers essential data about entities
	 */
	//private Entity entity;
	public short entityType;
	public long entityID;
	//World world;

	public double XBuffered, YBuffered, ZBuffered;
	public double RHBuffered, RVBuffered;
	public String nBuffered;

	public boolean defineControl = false; // Tells the client that the player entity is this one.
	public boolean includeRotation = false; // Tells both sides to consider extra 2 doubles
	public boolean includeName = false; // This is a nameable entity
	public boolean deleteFlag = false; // Tells client to stop tracking this entity and delete it

	public Packet04Entity(boolean client)
	{
		super(client);
	}

	@Override
	public void send(DataOutputStream out) throws IOException
	{
		//System.out.println("Sending entity " + entityID + " EID : " + entityType + " PosX" + XBuffered + (nBuffered == null ? "null" : nBuffered));
		out.writeByte(0x04);
		out.writeLong(entityID);
		out.writeShort(entityType);
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
		entityID = in.readLong();
		entityType = in.readShort();

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
		entity.posX = XBuffered;
		entity.posY = YBuffered;
		entity.posZ = ZBuffered;
		if (includeRotation)
		{
			entity.rotH = (float) RHBuffered;
			entity.rotV = (float) RVBuffered;
		}
		if (includeName)
		{
			System.out.println("apply 2 "+entity+" posx"+XBuffered+" -> "+nBuffered);
			if (entity instanceof EntityNameable)
				((EntityNameable) entity).setName(nBuffered);
		}
		//System.out.println("apply 2 "+entity+" posx"+XBuffered+" -> "+entity.posX);
	}

	public void applyFromEntity(Entity entity)
	{
		entityType = entity.getEID();
		entityID = entity.getUUID();

		XBuffered = entity.posX;
		YBuffered = entity.posY;
		ZBuffered = entity.posZ;
		if (includeRotation)
		{
			RHBuffered = entity.rotH;
			RVBuffered = entity.rotV;
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
			Entity entity = Client.world.getEntityByUUID(this.entityID);
			if(this.deleteFlag)
			{
				//System.out.println("Deleting Entity "+entity);
				Client.world.removeEntity(entity);
			}
			else
			{
				if(entity == null)
				{
					entity = EntitiesList.newEntity(Client.world, this.entityType);
					entity.entityID = this.entityID;
					this.applyToEntity(entity);
					Client.world.addEntity(entity);
					//System.out.println("Added entity "+entity);
					if(this.defineControl)
					{
						Client.controller = entity;
						//System.out.println("you should control this entity :"+entity);
					}
				}
				else
					this.applyToEntity(entity);
			}
		}
		else
		{
			if (processor.getServerClient().profile.entity != null && entityID == processor.getServerClient().profile.entity.entityID)
				applyToEntity(processor.getServerClient().profile.entity);
			//entity = EntitiesList.newEntity(world, entityType);
		}
	}
}
