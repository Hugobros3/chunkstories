package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.entity.core.components.EntityComponentInventory;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityWithInventory extends Entity
{	
	public EntityComponentInventory getInventory();
}
