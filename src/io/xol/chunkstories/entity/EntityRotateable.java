package io.xol.chunkstories.entity;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.entity.core.components.EntityComponentRotation;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public interface EntityRotateable extends Entity
{
	public EntityComponentRotation getEntityRotationComponent();
}
