package io.xol.chunkstories.api.entity.interfaces;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.core.entity.components.EntityComponentRotation;
import io.xol.engine.math.lalgb.vector.dp.Vector3dm;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public interface EntityRotateable extends Entity
{
	public EntityComponentRotation getEntityRotationComponent();
	
	public Vector3dm getDirectionLookingAt();
}
