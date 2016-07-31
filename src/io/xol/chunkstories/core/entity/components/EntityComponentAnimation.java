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

public class EntityComponentAnimation extends EntityComponent
{
	String currentAnimationName = "";
	boolean loopAnimation = false;
	long animationStartTimer = 0L;
	
	public EntityComponentAnimation(Entity entity)
	{
		super(entity);
	}

	public void startAnimation(String animationName, boolean loop)
	{	
		this.currentAnimationName = animationName;
		this.loopAnimation = loop;
		
		this.animationStartTimer = System.currentTimeMillis();
		
		this.pushComponentEveryone();
	}
	
	public String getAnimationName()
	{
		return currentAnimationName;
	}
	
	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeUTF(currentAnimationName);
		dos.writeBoolean(loopAnimation);
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		String currentAnimationName = dis.readUTF();
		boolean loopAnimation = dis.readBoolean();
	}

}
