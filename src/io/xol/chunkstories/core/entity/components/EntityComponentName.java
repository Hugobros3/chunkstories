package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.csf.StreamSource;
import io.xol.chunkstories.api.csf.StreamTarget;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentName extends EntityComponent
{
	public EntityComponentName(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}
	String name = "";
	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeUTF(name);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		name = dis.readUTF();
	}

}