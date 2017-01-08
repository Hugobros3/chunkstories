package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.world.World;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityType
{
	public String getName();
	
	public short getId();
	
	public Entity create(World world);
}
