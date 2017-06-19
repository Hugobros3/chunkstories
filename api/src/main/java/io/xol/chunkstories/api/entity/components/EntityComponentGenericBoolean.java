package io.xol.chunkstories.api.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class EntityComponentGenericBoolean extends EntityComponent
{
	private boolean value = false;

	public boolean get()
	{
		return value;
	}

	public void set(boolean newValue)
	{
		if (this.value != newValue)
		{
			this.value = newValue;
			this.pushComponentEveryone();
		}
	}

	public EntityComponentGenericBoolean(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeBoolean(value);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		value = dis.readBoolean();
	}

}
