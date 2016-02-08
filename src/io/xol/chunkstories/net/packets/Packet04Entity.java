package io.xol.chunkstories.net.packets;

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
		System.out.println("Sending entity "+entityID+" EID : "+entityType+" PosX"+XBuffered);
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
		if(includeName)
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
		if(includeName)
		{
			nBuffered = in.readUTF();
			System.out.println(nBuffered);
		}
	}

	public void applyToEntity(Entity entity)
	{
		entityType = entity.getEID();
		entityID = entity.getUUID();
		
		entity.posX = XBuffered;
		entity.posY = YBuffered;
		entity.posZ = ZBuffered;
		if (includeRotation)
		{
			entity.rotH = (float) RHBuffered;
			entity.rotV = (float) RVBuffered;
		}
		if(includeName)
		{
			if(entity instanceof EntityNameable)
				((EntityNameable)entity).setName(nBuffered);
		}
	}
	
	public void applyFromEntity(Entity entity)
	{
		XBuffered = entity.posX;
		YBuffered = entity.posX;
		ZBuffered = entity.posX;
		if (includeRotation)
		{
		RHBuffered = entity.rotH;
		RVBuffered = entity.rotV;
		}
		if(includeName)
		{
			if(entity instanceof EntityNameable)
				nBuffered = ((EntityNameable)entity).getName();
		}
	}
}
