package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentCreativeMode extends EntityComponent
{
	private boolean creativeMode = false;
	
	public boolean isCreativeMode()
	{
		return creativeMode;
	}

	public void setCreativeMode(boolean creativeMode)
	{
		this.creativeMode = creativeMode;
		this.pushComponentController();
	}

	public EntityComponentCreativeMode(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeBoolean(creativeMode);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		creativeMode = dis.readBoolean();
	}

}
