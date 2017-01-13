package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.core.entity.components.EntityComponentFlying;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityFlying extends Entity
{
	public EntityComponentFlying getFlyingComponent();
}
