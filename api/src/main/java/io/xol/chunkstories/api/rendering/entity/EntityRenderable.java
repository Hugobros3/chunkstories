package io.xol.chunkstories.api.rendering.entity;

import io.xol.chunkstories.api.entity.Entity;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface EntityRenderable extends Entity
{
	/**
	 * An EntityRenderable provides a EntityRenderer object to batch-render all entities of the same type
	 */
	public EntityRenderer<? extends EntityRenderable> getEntityRenderer();
}
