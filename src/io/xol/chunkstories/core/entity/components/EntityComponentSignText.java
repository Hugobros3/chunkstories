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

public class EntityComponentSignText extends EntityComponent
{
	public EntityComponentSignText(Entity entity, EntityComponent previous)
	{
		super(entity, previous);
	}
	
	String signTexture =  "Hitler did\n"
			+ "Nothing\n"
			+ "Wrong\n"
			+ "kappa";
	
	public String getSignText()
	{
		return signTexture;
	}

	public void setSignText(String name)
	{
		this.signTexture = name;
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeUTF(signTexture);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		signTexture = dis.readUTF();
	}
}
