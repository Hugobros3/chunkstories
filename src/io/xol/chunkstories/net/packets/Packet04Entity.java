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
	public Entity entity;
	public short entityType;
	public long entityID;
	//World world;

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
		//System.out.println("Sending entity "+entity+" EID : "+entity.getEID());
		out.writeByte(0x04);
		out.writeLong(entity.getUUID());
		out.writeShort(entity.getEID());
		out.writeDouble(entity.posX);
		out.writeDouble(entity.posY);
		out.writeDouble(entity.posZ);
		byte byteField = 0x00;
		byteField = (byte) (byteField | ((defineControl ? 0x01 : 0x00) << 0));
		byteField = (byte) (byteField | ((includeRotation ? 0x01 : 0x00) << 1));
		byteField = (byte) (byteField | ((includeName ? 0x01 : 0x00) << 2));
		byteField = (byte) (byteField | ((deleteFlag ? 0x01 : 0x00) << 3));
		out.writeByte(byteField);
		if (includeRotation)
		{
			out.writeDouble(entity.rotH);
			out.writeDouble(entity.rotV);
		}
		if(includeName)
		{
			String name = "ERROR-NOTNAMEABLE";
			if(entity instanceof EntityNameable)
				name = ((EntityNameable)entity).getName();
			out.writeUTF(name);
		}
	}

	double Xtemp, YTemp, ZTemp;
	double RHTemp, RVTemp;
	String nTemp;
	
	@Override
	public void read(DataInputStream in) throws IOException
	{
		entityID = in.readLong();
		entityType = in.readShort();

		Xtemp = in.readDouble();
		YTemp = in.readDouble();
		ZTemp = in.readDouble();
		
		byte byteField = in.readByte();
		defineControl = ((byteField >> 0) & 0x01) == 1;
		includeRotation = ((byteField >> 1) & 0x01) == 1;
		includeName = ((byteField >> 2) & 0x01) == 1;
		deleteFlag = ((byteField >> 3) & 0x01) == 1;
		
		if (includeRotation)
		{
			RHTemp = (float) in.readDouble();
			RVTemp = (float) in.readDouble();
		}
		if(includeName)
		{
			nTemp = in.readUTF();
			System.out.println(nTemp);
		}
	}

	public void applyToEntity(Entity entity, DataInputStream in) throws IOException
	{
		entity.posX = Xtemp;
		entity.posY = YTemp;
		entity.posZ = ZTemp;
		if (includeRotation)
		{
			entity.rotH = (float) RHTemp;
			entity.rotV = (float) RVTemp;
		}
		if(includeName)
		{
			if(entity instanceof EntityNameable)
				((EntityNameable)entity).setName(nTemp);
		}
	}

}
