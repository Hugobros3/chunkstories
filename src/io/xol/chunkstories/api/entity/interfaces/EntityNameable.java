package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.entity.core.components.EntityComponentName;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityNameable
{
	public abstract String getName();
	public abstract void setName(String n);
	
	public abstract EntityComponentName getNameComponent();
}
