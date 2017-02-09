package io.xol.chunkstories.core.entity.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.serialization.StreamSource;
import io.xol.chunkstories.api.serialization.StreamTarget;
import io.xol.chunkstories.core.entity.EntityHumanoid.EntityHumanoidStance;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class EntityComponentStance extends EntityComponent
{
	private EntityHumanoidStance value = EntityHumanoidStance.STANDING;
	
	public EntityHumanoidStance get()
	{
		return value;
	}

	public void set(EntityHumanoidStance flying)
	{
		this.value = flying;
		this.pushComponentEveryone();
	}

	public EntityComponentStance(Entity entity)
	{
		super(entity);
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeByte(this.value.ordinal());
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		value = EntityHumanoidStance.values()[dis.readByte()];
		this.pushComponentEveryoneButController();
	}

}
