package io.xol.chunkstories.entity.core.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.net.StreamSource;
import io.xol.chunkstories.api.net.StreamTarget;
import io.xol.chunkstories.api.world.WorldMaster;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentRotation extends EntityComponent
{
	private float rotH = 0f, rotV = 0f;
	
	public EntityComponentRotation(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	public float getRotH()
	{
		return rotH;
	}
	
	public float getRotV()
	{
		return rotV;
	}
	
	public void setRotation(float rotH, float rotV)
	{
		this.rotH = rotH;
		this.rotV = rotV;
		
		this.pushComponentEveryone();
	}
	
	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeFloat(rotH);
		dos.writeFloat(rotV);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		rotH = dis.readFloat();
		rotV = dis.readFloat();

		//Position updates received by the server should be told to everyone but the controller
		if(entity.getWorld() instanceof WorldMaster)
			this.pushComponentEveryoneButController();
	}

}
