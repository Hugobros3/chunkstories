package io.xol.chunkstories.entity.core.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.net.StreamTarget;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentFlying extends EntityComponent
{
	private boolean flying = false;
	
	public boolean isFlying()
	{
		return flying;
	}

	public void setFlying(boolean flying)
	{
		this.flying = flying;
		this.pushComponentController();
	}

	public EntityComponentFlying(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeBoolean(flying);
	}

	@Override
	protected void pull(DataInputStream dis) throws IOException
	{
		flying = dis.readBoolean();
	}

}
